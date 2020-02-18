package io.github.edmm.plugins.multi;

import io.github.edmm.core.plugin.AbstractLifecycle;
import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.Artifact;
import io.github.edmm.model.Operation;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.plugins.multi.model_extensions.OrchestrationTechnologyMapping;
import io.github.edmm.plugins.multi.orchestration.AnsibleOrchestratorVisitor;
import io.github.edmm.plugins.multi.orchestration.KubernetesOrchestratorVisitor;
import io.github.edmm.plugins.multi.orchestration.TerraformOrchestratorVisitor;
import org.apache.commons.io.FilenameUtils;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;

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

        try {
            fileAccess.write("plan.plan", "here is the plan");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Reverse the graph to find sources
        EdgeReversedGraph<RootComponent, RootRelation> dependencyGraph = new EdgeReversedGraph<>(
                context.getModel().getTopology());
        // Apply the topological sort
        TopologicalOrderIterator<RootComponent, RootRelation> iterator = new TopologicalOrderIterator<>(
                dependencyGraph);

        int i = 0;
        while (iterator.hasNext()) {

            RootComponent comp = iterator.next();
            context.setSubFileAcess(comp.getNormalizedName());
            try {
                fileAccess.append("plan.plan", i + ": " + comp.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }

            Optional<Map<RootComponent, Technology>> deploymentTechList = context.getModel().getTechnologyMapping()
                    .map(OrchestrationTechnologyMapping::getTechForComponents);
            Technology deploy = deploymentTechList.map(c -> c.get(comp)).orElse(Technology.UNDEFINED);

            // TODO better version
            logger.info("deployment_tool: {} ", deploy);
            if (deploy == Technology.ANSIBLE) {

                AnsibleVisitor ansibleVisitor = new AnsibleVisitor(context);
                comp.accept(ansibleVisitor);
            } else if (deploy == Technology.TERRAFORM) {
                TerraformVisitor visitor = new TerraformVisitor(context);

                comp.accept(visitor);
            } else if (deploy == Technology.KUBERNETES) {
                KubernetesVisitor kubernetesVisitor = new KubernetesVisitor(context);
                comp.accept(kubernetesVisitor);
            } else {
                logger.error("could not find technology: {} for component {}", deploy, comp.getNormalizedName());
            }
            logger.info("{}", comp.getName());

            i++;

        }

        Writer writer = new StringWriter();
        context.getModel().getGraph().generateYamlOutput(writer);

        try {
            fileAccess.write("state.yaml", writer.toString());
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

        int i = 0;
        while (iterator.hasNext()) {

            RootComponent comp = iterator.next();
            // give this component its own folder
            context.setSubFileAcess(comp.getNormalizedName());

            Optional<Map<RootComponent, Technology>> deploymentTechList = context.getModel().getTechnologyMapping()
                    .map(OrchestrationTechnologyMapping::getTechForComponents);
            // use technology or ansible as default for now
            Technology deploy = deploymentTechList.map(c -> c.get(comp)).orElse(Technology.ANSIBLE);

            // TODO clean version
            logger.info("deployment_tool: {} ", deploy);
            if (deploy == Technology.ANSIBLE) {
                AnsibleOrchestratorVisitor ansibleVisitor = new AnsibleOrchestratorVisitor(context);
                comp.accept(ansibleVisitor);
            } else if (deploy == Technology.TERRAFORM) {
                TerraformOrchestratorVisitor terraformVisitor = new TerraformOrchestratorVisitor(context);
                comp.accept(terraformVisitor);
            } else if (deploy == Technology.KUBERNETES) {
                KubernetesOrchestratorVisitor kubernetesVisitor = new KubernetesOrchestratorVisitor(context);
                comp.accept(kubernetesVisitor);
            }
            logger.info("{}", comp.getName());

            i++;

        }

        try {

            Writer writer = new StringWriter();
            context.getModel().getGraph().generateYamlOutput(writer);
            fileAccess.write("state.yaml", writer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
