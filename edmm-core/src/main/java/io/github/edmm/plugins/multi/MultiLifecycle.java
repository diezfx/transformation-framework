package io.github.edmm.plugins.multi;

import com.google.gson.Gson;
import io.github.edmm.core.plugin.AbstractLifecycle;
import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.plugins.multi.model_extensions.groupingGraph.Group;
import io.github.edmm.plugins.multi.orchestration.*;
import io.github.edmm.plugins.multi.model.ComponentResources;
import io.github.edmm.plugins.multi.model.Plan;
import io.github.edmm.plugins.multi.model.PlanStep;
import lombok.var;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

public class MultiLifecycle extends AbstractLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(MultiLifecycle.class);

    public MultiLifecycle(TransformationContext context) {
        super(context);
    }

    // prepare the graph
    @Override
    public void prepare() {

    }


    public void createPlan(List<Group> sortedGroups) {

        EdgeReversedGraph<RootComponent, RootRelation> dependencyGraph = new EdgeReversedGraph<>(
                context.getModel().getTopology());
        TopologicalOrderIterator<RootComponent, RootRelation> iterator = new TopologicalOrderIterator<>(
                dependencyGraph);


        List<PlanStep> stepList = new ArrayList<>();
        List<MultiVisitor> contextList = new ArrayList<>();
        List<Group> groups = new ArrayList<>();

        //init contexts
        MultiVisitor visitorContext;

        for (var group : sortedGroups) {
            if (group.getTechnology() == Technology.ANSIBLE) {
                visitorContext = new AnsibleVisitor(context);
            } else if (group.getTechnology() == Technology.TERRAFORM) {
                visitorContext = new TerraformVisitor(context);
            } else if (group.getTechnology() == Technology.KUBERNETES) {
                visitorContext = new KubernetesVisitor(context);
            } else {
                String error = String.format("could not find technology: %s for components %s", group.getTechnology(), group);
                throw new IllegalArgumentException(error);
            }
            groups.add(group);
            contextList.add(visitorContext);
            stepList.add(new PlanStep(group.getTechnology()));
        }



        while (iterator.hasNext()) {

            RootComponent comp = iterator.next();
            int index = -1;
            for (int i = 0; i < groups.size(); i++) {
                if (groups.get(i).getGroupComponents().contains(comp)) {
                    index = i;
                    break;
                }
            }
            context.setSubFileAcess("step" + index + "_" + context.getModel().getTechnology(comp));
            // this has to happen first at the moment; otherwise the announced output vars from comp are set as input as well
            var propList = TransformationHelper.collectRuntimeEnvVars(context.getTopologyGraph(), comp);
            stepList.get(index).components.add(new ComponentResources(comp.getName(), propList));


            comp.accept(contextList.get(index));


        }

        Plan plan = new Plan();
        plan.steps = stepList;

        for (int i = 0; i < contextList.size(); i++) {
            context.setSubFileAcess("step" + i + "_" + stepList.get(i).tech.toString());
            contextList.get(i).populate();
        }
        Writer writer = new StringWriter();
        context.getModel().getGraph().generateYamlOutput(writer);

        try {
            context.getFileAccess().write("state.yaml", writer.toString());
            context.getFileAccess().write("execution.plan.json", plan.toJson());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void transform() {
        logger.info("Begin transformation to Multi...");
        //new Groupprovisioning
        List<Group> sortedGroups = GroupProvisioning.determineProvisiongingOrder(context.getModel());
        createPlan(sortedGroups);


        // VisitorHelper.visit(context.getModel().getComponents(), visitor, component ->
        // component instanceof Compute);
        // ... then all others
        // VisitorHelper.visit(context.getModel().getComponents(), visitor);
        // VisitorHelper.visit(context.getModel().getRelations(), visitor);


        logger.info("Transformation to Multi successful");
    }

    @Override
    public void orchestrate() {
        PluginFileAccess fileAccess = context.getFileAccess();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter y to continue with orchestration");
        String input = scanner.next();

        if (!input.equals("y")) {
            return;
        }
        var gson = new Gson();
        try {

            Plan plan = gson.fromJson(context.getFileAccess().readToStringTargetDir("execution.plan.json"), Plan.class);


            logger.info("Begin orchestration ...");

            for (int i = 0; i < plan.steps.size(); i++) {
                List<RootComponent> components = new ArrayList<>();
                for (var c : plan.steps.get(i).components) {
                    Optional<RootComponent> comp = context.getModel().getComponent(c.getName());
                    comp.ifPresent(components::add);
                }
                Technology tech = plan.steps.get(i).tech;
                context.setSubFileAcess("step" + i + "_" + tech.toString());

                GroupVisitor visitorContext;
                logger.info("deployment_tool: {} ", tech);
                if (tech == Technology.ANSIBLE) {
                    visitorContext = new AnsibleOrchestratorVisitor(context);
                } else if (tech == Technology.TERRAFORM) {
                    visitorContext = new TerraformOrchestratorVisitor(context);
                } else if (tech == Technology.KUBERNETES) {
                    visitorContext = new KubernetesOrchestratorVisitor(context);
                } else {
                    String error = String.format("could not find technology: %s for component %s", tech, components);
                    throw new IllegalArgumentException(error);
                }
                var deployInfo = components.stream()
                        .map(c -> new DeploymentModelInfo(c, getComputedProperties(c)))
                        .collect(Collectors.toList());
                visitorContext.visit(deployInfo);
            }
            Writer writer = new StringWriter();
            context.getModel().getGraph().generateYamlOutput(writer);
            fileAccess.write("state.yaml", writer.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Map<String, Property> getComputedProperties(RootComponent component) {
        Map<String, Property> allProps = TopologyGraphHelper.findAllProperties(context.getTopologyGraph(), component);
        Map<String, Property> computedProps = new HashMap<>();
        for (var prop : allProps.entrySet()) {
            if (prop.getValue().isComputed() || prop.getValue().getValue() == null || prop.getValue().getValue().startsWith("$")) {
                computedProps.put(prop.getKey(), prop.getValue());
            }
        }
        return TopologyGraphHelper.resolveAllPropertyReferences(context.getTopologyGraph(), component, computedProps);
    }




}
