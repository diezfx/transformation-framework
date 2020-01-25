package io.github.edmm.plugins.multi;

import io.github.edmm.core.plugin.AbstractLifecycle;
import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.Artifact;
import io.github.edmm.model.Operation;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.plugins.multi.orchestration.AnsibleOrchestratorVisitor;
import io.github.edmm.plugins.multi.orchestration.TerraformOrchestratorVisitor;
import org.apache.commons.io.FilenameUtils;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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
        TerraformVisitor visitor = new TerraformVisitor(context);
        AnsibleVisitor ansibleVisitor = new AnsibleVisitor(context);

        //context.getModel().getGraph().generateYamlOutput(new OutputStreamWriter(System.out));

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
            //copy files otherwise have to do in every step?
            copyFiles(comp);

            String deploy = comp.getDeploymentTool().get();

            //TODO clean version
            logger.info("deployment_tool: {} ", deploy);
            if (deploy.equals("ansible")) {
                comp.accept(ansibleVisitor);
            } else {
                comp.accept(visitor);
            }
            logger.info("{}", comp.getName());

            i++;

            //reset for next round
            visitor = new TerraformVisitor(context);
            ansibleVisitor = new AnsibleVisitor(context);
        }


        Writer writer = new StringWriter();
        context.getModel().getGraph().generateYamlOutput(writer);

        try {
            fileAccess.write("state.yaml", writer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //VisitorHelper.visit(context.getModel().getComponents(), visitor, component -> component instanceof Compute);
        // ... then all others
        //VisitorHelper.visit(context.getModel().getComponents(), visitor);
        //VisitorHelper.visit(context.getModel().getRelations(), visitor);
        logger.info("Transformation to Multi successful");
    }


    @Override
    public void orchestrate() {
        PluginFileAccess fileAccess = context.getFileAccess();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter y to continue with orchestration");
        String input = scanner.next();

        if (input.equals("y") == false) {
            return;
        }


        logger.info("Begin orchestration ...");
        TerraformOrchestratorVisitor terraformVisitor = new TerraformOrchestratorVisitor(context);
        AnsibleOrchestratorVisitor ansibleVisitor = new AnsibleOrchestratorVisitor(context);
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

            String deploy = comp.getDeploymentTool().get();

            //TODO clean version
            logger.info("deployment_tool: {} ", deploy);
            if (deploy.equals("ansible")) {
                comp.accept(ansibleVisitor);
            } else {
                comp.accept(terraformVisitor);
            }
            logger.info("{}", comp.getName());

            i++;

            //reset for next round
            terraformVisitor = new TerraformOrchestratorVisitor(context);
            ansibleVisitor = new AnsibleOrchestratorVisitor(context);
        }


        try {

            Writer writer = new StringWriter();
            context.getModel().getGraph().generateYamlOutput(writer);
            fileAccess.write("state.yaml", writer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void copyFiles(RootComponent comp) {
        PluginFileAccess fileAccess = context.getSubDirAccess();
        for (Artifact artifact : comp.getArtifacts()) {
            try {
                //get basename
                String basename = FilenameUtils.getName(artifact.getValue());
                String newPath = "./files/" + basename;
                fileAccess.copy(artifact.getValue(), newPath);
                artifact.setValue(newPath);
            } catch (IOException e) {
                logger.warn("Failed to copy file '{}'", artifact.getValue());
            }

        }
        List<Artifact> operations = collectOperations(comp);

        for (Artifact artifact : operations) {
            try {
                String basename = FilenameUtils.getName(artifact.getValue());
                String newPath = "./files/" + basename;
                fileAccess.copy(artifact.getValue(), newPath);
                artifact.setValue(newPath);
            } catch (IOException e) {
                logger.warn("Failed to copy file '{}'", artifact.getValue());
            }

        }


    }

    private List<Artifact> collectOperations(RootComponent component) {
        List<Artifact> operations = new ArrayList<>();
        Consumer<Operation> artifactsConsumer = op -> operations.addAll(op.getArtifacts());
        component.getStandardLifecycle().getCreate().ifPresent(artifactsConsumer);
        component.getStandardLifecycle().getConfigure().ifPresent(artifactsConsumer);
        component.getStandardLifecycle().getStart().ifPresent(artifactsConsumer);
        return operations;
    }

}
