package io.github.edmm.plugins.mixed_deployment;

import freemarker.template.Configuration;
import io.github.edmm.core.plugin.BashScript;
import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.plugin.TemplateHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.core.transformation.TransformationException;
import io.github.edmm.model.Artifact;
import io.github.edmm.model.Operation;
import io.github.edmm.model.component.Compute;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.model.visitor.RelationVisitor;
import io.github.edmm.plugins.terraform.model.FileProvisioner;
import io.github.edmm.plugins.terraform.model.RemoteExecProvisioner;
import io.github.edmm.plugins.terraform.model.aws.Ec2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiVisitor implements ComponentVisitor, RelationVisitor {

    protected final TransformationContext context;
    protected final Configuration cfg = TemplateHelper.forClasspath(MultiPlugin.class, "/plugins/mixed_deployment");
    protected final Graph<RootComponent, RootRelation> graph;

    private static final Logger logger = LoggerFactory.getLogger(MultiVisitor.class);

    public MultiVisitor(TransformationContext context) {
        this.context = context;
        this.graph = context.getTopologyGraph();
    }

    public void populateFiles() {

    };

    @Override
    public void visit(Compute component) {

        Ec2 ec2 = Ec2.builder().name(component.getNormalizedName())
                // TODO: Try to resolve image
                .ami("ami-0bbc25e23a7640b9b")
                // TODO: Try to resolve instance type
                .instanceType("t2.micro").build();
        List<String> operations = collectOperations(component);
        ec2.addRemoteExecProvisioner(new RemoteExecProvisioner(operations));
        ec2.addFileProvisioner(new FileProvisioner("./env.sh", "/opt/env.sh"));

        PluginFileAccess fileAccess = context.getFileAccess();
        BashScript envScript = new BashScript(fileAccess, "env.sh");
        Map<String, Object> data = new HashMap<>();
        data.put("ec2", ec2);

        try {
            fileAccess.append("compute.tf", TemplateHelper.toString(cfg, "compute.tf", data));
        } catch (IOException e) {
            logger.error("Failed to write Terraform file", e);
            throw new TransformationException(e);
        }

        component.setTransformed(true);
    }

    @Override
    public void visit(RootComponent component) {

        List<String> operations = collectOperations(component);
        List<FileProvisioner> files = collectFileProvisioners(component);

        RemoteExecProvisioner executions = new RemoteExecProvisioner(operations);

        PluginFileAccess fileAccess = context.getFileAccess();
        Map<String, Object> data = new HashMap<>();
        data.put("name", component.getName());
        data.put("files",files);
        data.put("operations", executions);

        try {
            fileAccess.append("software.tf", TemplateHelper.toString(cfg, "software.tf", data));
        } catch (IOException e) {
            logger.error("Failed to write Terraform file", e);
            throw new TransformationException(e);
        }

        component.setTransformed(true);
    }

    private List<FileProvisioner> collectFileProvisioners(RootComponent component) {
        List<FileProvisioner> files = new ArrayList<FileProvisioner>();
        for (Artifact artifact : component.getArtifacts()) {
            String destination = "/opt/" + component.getNormalizedName();

            files.add(new FileProvisioner(artifact.getValue(), destination));
        }
        return files;
    }

    private List<String> collectOperations(RootComponent component) {
        List<String> operations = new ArrayList<>();
        Consumer<Operation> artifactsConsumer = op -> op.getArtifacts()
                .forEach(artifact -> operations.add(artifact.getValue()));
        component.getStandardLifecycle().getCreate().ifPresent(artifactsConsumer);
        component.getStandardLifecycle().getConfigure().ifPresent(artifactsConsumer);
        component.getStandardLifecycle().getStart().ifPresent(artifactsConsumer);
        return operations;
    }
}
