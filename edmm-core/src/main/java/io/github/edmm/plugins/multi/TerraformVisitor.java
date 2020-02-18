package io.github.edmm.plugins.multi;

import freemarker.template.Configuration;
import io.github.edmm.core.plugin.BashScript;
import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.plugin.TemplateHelper;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.core.transformation.TransformationException;
import io.github.edmm.model.Artifact;
import io.github.edmm.model.Operation;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.*;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.model.visitor.RelationVisitor;
import io.github.edmm.plugins.terraform.model.Aws;
import io.github.edmm.plugins.terraform.model.FileProvisioner;
import io.github.edmm.plugins.terraform.model.RemoteExecProvisioner;
import org.apache.commons.io.FilenameUtils;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class TerraformVisitor implements ComponentVisitor, RelationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(TerraformVisitor.class);
    protected final TransformationContext context;
    protected final Configuration cfg = TemplateHelper.forClasspath(MultiPlugin.class, "/plugins/multi");
    protected final Graph<RootComponent, RootRelation> graph;

    public TerraformVisitor(TransformationContext context) {
        this.context = context;
        this.graph = context.getTopologyGraph();
    }


    @Override
    public void visit(Compute component) {
        //todo dont use aws instance for openstack
        Aws.Instance ec2 = Aws.Instance.builder().name(component.getNormalizedName())
                .privKeyFile(component.getPrivateKeyPath().get()).build();
        List<String> operations = collectOperations(component);

        // add properties that are needed for this component to work
        component.addProperty("hostname", null);

        ec2.addRemoteExecProvisioner(new RemoteExecProvisioner(operations));

        PluginFileAccess fileAccess = context.getSubDirAccess();
        Map<String, Object> data = new HashMap<>();
        data.put("ec2", ec2);

        String filename = String.format("%s.tf", component.getNormalizedName());
        try {
            fileAccess.write(filename, TemplateHelper.toString(cfg, "compute.tf", data));
        } catch (IOException e) {
            logger.error("Failed to write Terraform file", e);
            throw new TransformationException(e);
        }

        component.setTransformed(true);
    }

    @Override
    public void visit(RootComponent component) {

        Optional<Compute> computeSource = TopologyGraphHelper.resolveHostingComputeComponent(graph, component);
        List<String> operations = collectOperations(component);
        List<FileProvisioner> files = collectFileProvisioners(component);
        RemoteExecProvisioner executions = new RemoteExecProvisioner(operations);
        PluginFileAccess fileAccess = context.getSubDirAccess();

        // set static env variables
        String envScriptName = String.format("%s_env.sh", component.getNormalizedName());
        BashScript envScript = new BashScript(fileAccess, envScriptName);
        Map<String, String> envVars = collectEnvVars(component);
        if (envVars.size() > 0) {
            envVars.forEach((name, value) -> envScript.append("export " + name + "=" + value));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("name", component.getName());
        data.put("files", files);
        data.put("operations", executions);
        data.put("envScript", envScriptName);
        data.put("compute", computeSource.get().getName());

        String filename = String.format("%s.tf", component.getNormalizedName());

        try {
            fileAccess.write(filename, TemplateHelper.toString(cfg, "software.tf", data));
        } catch (IOException e) {
            logger.error("Failed to write Terraform file", e);
            throw new TransformationException(e);
        }

        component.setTransformed(true);
    }

    private List<FileProvisioner> collectFileProvisioners(RootComponent component) {
        List<FileProvisioner> files = new ArrayList<FileProvisioner>();
        String destination = "/opt/" + component.getNormalizedName() + "/";
        for (Artifact artifact : component.getArtifacts()) {
            files.add(new FileProvisioner(artifact.getValue(), destination));
        }
        return files;
    }

    private List<String> collectOperations(RootComponent component) {
        List<String> operations = new ArrayList<>();
        Consumer<Operation> artifactsConsumer = op -> op.getArtifacts()
                .forEach(artifact -> operations.add("./files/" + FilenameUtils.getName(artifact.getValue())));
        component.getStandardLifecycle().getCreate().ifPresent(artifactsConsumer);
        component.getStandardLifecycle().getConfigure().ifPresent(artifactsConsumer);
        component.getStandardLifecycle().getStart().ifPresent(artifactsConsumer);
        return operations;
    }

    private Map<String, String> collectEnvVars(RootComponent component) {

        Map<String, String> envVars = new HashMap<>();
        String[] blacklist = {"key_name", "public_key"};
        component.getProperties().values().stream().filter(p -> !Arrays.asList(blacklist).contains(p.getName()))
                .filter(p -> p.getValue() != null || p.isComputed() == false).forEach(p -> {
            String name = (component.getNormalizedName() + "_" + p.getNormalizedName()).toUpperCase();
            envVars.put(name, p.getValue());
        });

        return envVars;
    }


    @Override
    public void visit(Tomcat component) {
        visit((RootComponent) component);
    }

    @Override
    public void visit(MysqlDbms component) {
        visit((RootComponent) component);
    }

    @Override
    public void visit(Database component) {
        visit((RootComponent) component);
    }

    @Override
    public void visit(Dbms component) {
        visit((RootComponent) component);
    }

    @Override
    public void visit(MysqlDatabase component) {
        visit((RootComponent) component);
    }

    @Override
    public void visit(WebApplication component) {
        visit((RootComponent) component);
    }

}
