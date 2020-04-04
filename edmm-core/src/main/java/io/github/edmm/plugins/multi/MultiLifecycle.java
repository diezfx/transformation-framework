package io.github.edmm.plugins.multi;

import com.google.gson.Gson;
import io.github.edmm.core.plugin.AbstractLifecycle;
import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.plugins.multi.model.ComponentResources;
import io.github.edmm.plugins.multi.model.Plan;
import io.github.edmm.plugins.multi.model.PlanStep;
import io.github.edmm.plugins.multi.model_extensions.groupingGraph.Group;
import io.github.edmm.plugins.multi.orchestration.*;
import lombok.var;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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


        Plan plan = new Plan();

        for (int i = 0; i < sortedGroups.size(); i++) {
            var group = sortedGroups.get(i);

            var subgraph = new AsSubgraph<>(dependencyGraph, group.getGroupComponents());
            logger.info(subgraph.toString());
            TopologicalOrderIterator<RootComponent, RootRelation> subIterator = new TopologicalOrderIterator<>(
                    subgraph);


            //init contexts
            MultiVisitor visitorContext;
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


            var step = new PlanStep(group.getTechnology());
            context.setSubFileAcess("step" + i + "_" + group.getTechnology());
            while (subIterator.hasNext()) {
                RootComponent comp = subIterator.next();

                // this has to happen first at the moment; otherwise the announced output vars from comp are set as input as well
                var propList = TransformationHelper.collectRuntimeEnvVars(context.getTopologyGraph(), comp);
                step.components.add(new ComponentResources(comp.getName(), propList));
                comp.accept(visitorContext);
            }

            plan.steps.add(step);
            visitorContext.populate();

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


        // ... what to do with relations?
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

                var groupFileAccess=new File(context.getTargetDirectory(),"step" + i + "_" + tech.toString());

                var deployInfo = components.stream()
                        .map(c -> new DeploymentModelInfo(c, getComputedProperties(c)))
                        .collect(Collectors.toList());

                GroupVisitor visitorContext;
                var orchContext=new OrchestrationContext(groupFileAccess,context.getModel());
                // at the moment they need access to the file access and graph(this could be changed)
                logger.info("deployment_tool: {} ", tech);
                if (tech == Technology.ANSIBLE) {
                    visitorContext = new AnsibleOrchestratorVisitor(orchContext);
                } else if (tech == Technology.TERRAFORM) {
                    visitorContext = new TerraformOrchestratorVisitor(orchContext);
                } else if (tech == Technology.KUBERNETES) {
                    visitorContext = new KubernetesOrchestratorVisitor(orchContext);
                } else {
                    String error = String.format("could not find technology: %s for component %s", tech, components);
                    throw new IllegalArgumentException(error);
                }
                visitorContext.execute(deployInfo);

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
