package io.github.edmm.plugins.multi;

import io.github.edmm.core.plugin.AbstractLifecycle;
import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.HostedOn;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.plugins.multi.model_extensions.OrchestrationTechnologyMapping;
import io.github.edmm.plugins.multi.orchestration.*;
import lombok.var;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public class MultiLifecycle extends AbstractLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(MultiLifecycle.class);

    public MultiLifecycle(TransformationContext context) {
        super(context);
    }

    // prepare the graph
    @Override
    public void prepare() {

    }

    @Override
    public void transform() {
        logger.info("Begin transformation to Multi...");
        PluginFileAccess fileAccess = context.getFileAccess();
        Plan plan = new Plan();

        // Reverse the graph to find sources
        EdgeReversedGraph<RootComponent, RootRelation> dependencyGraph = new EdgeReversedGraph<>(
                context.getModel().getTopology());
        // Apply the topological sort
        TopologicalOrderIterator<RootComponent, RootRelation> iterator = new TopologicalOrderIterator<>(
                dependencyGraph);

        while (iterator.hasNext()) {

            RootComponent comp = iterator.next();
            Technology tech = getTechnology(comp);
            PlanStep step = new PlanStep(tech);
            var optTarget = TopologyGraphHelper.getTargetComponents(dependencyGraph, comp, HostedOn.class);
            // only do sth if this is a top component of this technology
            if (!optTarget.isEmpty() && getTechnology(optTarget.iterator().next()) == tech) {
                continue;
            }

            context.setSubFileAcess(comp.getNormalizedName());
            MultiVisitor visitorContext;

            // TODO better version
            logger.info("deployment_tool: {} ", tech);
            if (tech == Technology.ANSIBLE) {

                visitorContext = new AnsibleVisitor(context);

            } else if (tech == Technology.TERRAFORM) {
                visitorContext = new TerraformVisitor(context);

            } else if (tech == Technology.KUBERNETES) {
                visitorContext = new KubernetesVisitor(context);
            } else {
                String error = String.format("could not find technology: %s for component %s", tech, comp.getNormalizedName());
                throw new IllegalArgumentException(error);
            }

            List<RootComponent> sources = new ArrayList<>();
            sources.add(comp);

            // check if source uses same technology, then don't populate in this step
            var source = TopologyGraphHelper.getSourceComponents(dependencyGraph, comp, HostedOn.class).stream().findFirst();
            while (source.isPresent() && getTechnology(source.get()) == tech) {
                sources.add(source.get());
                source = TopologyGraphHelper.getSourceComponents(dependencyGraph, source.get(), HostedOn.class).stream().findFirst();
            }

            context.setSubFileAcess(comp.getNormalizedName());
            // visit all comps
            Collections.reverse(sources);
            for (var t : sources) {
                t.accept(visitorContext);
                step.components.add(t.getNormalizedName());
            }
            visitorContext.populate();
            plan.steps.add(step);
            logger.info("{}", comp.getName());

        }

        Writer writer = new StringWriter();
        context.getModel().getGraph().generateYamlOutput(writer);

        try {
            fileAccess.write("state.yaml", writer.toString());
            fileAccess.write("execution.plan", plan.toJson());
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        logger.info("Begin orchestration ...");
        // Reverse the graph to find sources
        EdgeReversedGraph<RootComponent, RootRelation> dependencyGraph = new EdgeReversedGraph<>(
                context.getModel().getTopology());
        // Apply the topological sort
        TopologicalOrderIterator<RootComponent, RootRelation> iterator = new TopologicalOrderIterator<>(
                dependencyGraph);

        while (iterator.hasNext()) {
            RootComponent comp = iterator.next();
            // see if this component is top component of this technology
            var optTarget = TopologyGraphHelper.getTargetComponents(dependencyGraph, comp, HostedOn.class);
            Technology tech = getTechnology(comp);
            if (!optTarget.isEmpty() && tech == getTechnology(optTarget.iterator().next())) {
                continue;
            }
            // give this component its own folder
            context.setSubFileAcess(comp.getNormalizedName());


            ComponentVisitor visitorContext;
            logger.info("deployment_tool: {} ", tech);
            if (tech == Technology.ANSIBLE) {
                visitorContext = new AnsibleOrchestratorVisitor(context);
            } else if (tech == Technology.TERRAFORM) {
                visitorContext = new TerraformOrchestratorVisitor(context);
            } else if (tech == Technology.KUBERNETES) {
                visitorContext = new KubernetesOrchestratorVisitor(context);
            } else {
                String error = String.format("could not find technology: %s for component %s", tech, comp.getNormalizedName());
                throw new IllegalArgumentException(error);
            }
            comp.accept(visitorContext);
            logger.info("{}", comp.getName());

        }

        try {
            Writer writer = new StringWriter();
            context.getModel().getGraph().generateYamlOutput(writer);
            fileAccess.write("state.yaml", writer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private Technology getTechnology(RootComponent component) {
        Optional<Map<RootComponent, Technology>> deploymentTechList = context.getModel().getTechnologyMapping()
                .map(OrchestrationTechnologyMapping::getTechForComponents);
        return deploymentTechList.map(c -> c.get(component)).orElse(Technology.UNDEFINED);
    }


}
