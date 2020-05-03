package io.github.edmm.plugins.multi;

import com.google.gson.Gson;
import io.github.edmm.core.plugin.AbstractLifecycle;
import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.plugin.PluginService;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.plugin.support.CheckModelResult;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.core.transformation.TransformationException;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.plugins.multi.ansible.AnsibleAreaLifecycle;
import io.github.edmm.plugins.multi.ansible.AnsibleExecutor;
import io.github.edmm.plugins.multi.kubernetes.KubernetesAreaLifecycle;
import io.github.edmm.plugins.multi.kubernetes.KubernetesExecutor;
import io.github.edmm.plugins.multi.model.ComponentResources;
import io.github.edmm.plugins.multi.model.Plan;
import io.github.edmm.plugins.multi.model.PlanStep;
import io.github.edmm.plugins.multi.model_extensions.groupingGraph.Group;
import io.github.edmm.plugins.multi.orchestration.ExecutionCompInfo;
import io.github.edmm.plugins.multi.orchestration.ExecutionContext;
import io.github.edmm.plugins.multi.orchestration.GroupExecutor;
import io.github.edmm.plugins.multi.terraform.TerraformAreaLifecycle;
import io.github.edmm.plugins.multi.terraform.TerraformExecutor;
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

    private final List<AbstractLifecycle> groupLifecycles;


    public MultiLifecycle(TransformationContext context) {

        super(context);
        groupLifecycles = new ArrayList<>();
    }

    // prepare the graph
    @Override
    public void prepare() {

    }


    public void createWorkflow(List<Group> sortedGroups) {

        EdgeReversedGraph<RootComponent, RootRelation> dependencyGraph = new EdgeReversedGraph<>(
            context.getModel().getTopology());


        Plan plan = new Plan();

        for (int i = 0; i < sortedGroups.size(); i++) {
            var group = sortedGroups.get(i);
            var subgraph = new AsSubgraph<>(dependencyGraph, group.getGroupComponents());
            TopologicalOrderIterator<RootComponent, RootRelation> subIterator = new TopologicalOrderIterator<>(
                subgraph);
            group.setSubGraph(subgraph);


            //init contexts
            AbstractLifecycle grpLifecycle;
            String subDir = "step" + i + "_" + group.getTechnology().toString();
            File targetDir=new File(context.getTargetDirectory(),subDir);
            var groupContext = new TransformationContext(subDir,context.getModel(),context.getTargetTechnology(),context.getSourceDirectory(),targetDir, group);


            if (group.getTechnology() == Technology.ANSIBLE) {
                grpLifecycle = new AnsibleAreaLifecycle(groupContext);
            } else if (group.getTechnology() == Technology.TERRAFORM) {
                grpLifecycle = new TerraformAreaLifecycle(groupContext);
            } else if (group.getTechnology() == Technology.KUBERNETES) {
                grpLifecycle = new KubernetesAreaLifecycle(groupContext);
            } else {
                String error = String.format("could not find technology: %s for components %s", group.getTechnology(), group);
                throw new IllegalArgumentException(error);
            }
            groupLifecycles.add(grpLifecycle);


            var step = new PlanStep(group.getTechnology());

            while (subIterator.hasNext()) {
                RootComponent comp = subIterator.next();
                var propList = TransformationHelper.collectRuntimeEnvVars(context.getTopologyGraph(), comp);
                step.components.add(new ComponentResources(comp.getName(), propList));
            }

            plan.steps.add(step);

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

        createWorkflow(sortedGroups);

        // suboptimally do 3 lifecycle step for groups here
        for (int i=0;i<groupLifecycles.size();i++) {
            var result = groupLifecycles.get(i).checkModel();
            if (result.getState() == CheckModelResult.State.UNSUPPORTED_COMPONENTS) {
                throw new TransformationException(String.format("unspported components  %s", sortedGroups.get(i).getTechnology()));
            }
            groupLifecycles.get(i).prepare();
            groupLifecycles.get(i).transform();
        }

        logger.info("Transformation to Multi successful");
    }

    //this could be another lifecycle step, but is planned to be completely independent in the future
    @Override
    public void execute() {
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

                var groupFileAccess=new File(context.getTargetDirectory(),"step" + i + "_" + tech.toString());
                var deployInfo = components.stream()
                    .map(c -> new ExecutionCompInfo(c, getComputedProperties(c)))
                    .collect(Collectors.toList());

                GroupExecutor visitorContext;
                var orchContext = new ExecutionContext(groupFileAccess, context.getModel());
                // at the moment they need access to the file access and graph(this could be changed)
                logger.info("deployment_tool: {} ", tech);
                if (tech == Technology.ANSIBLE) {
                    visitorContext = new AnsibleExecutor(orchContext);
                } else if (tech == Technology.TERRAFORM) {
                    visitorContext = new TerraformExecutor(orchContext);
                } else if (tech == Technology.KUBERNETES) {
                    visitorContext = new KubernetesExecutor(orchContext);
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
