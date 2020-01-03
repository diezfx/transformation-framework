package io.github.edmm.plugins.multi;

import com.google.common.collect.Lists;
import freemarker.template.Configuration;
import freemarker.template.Template;
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
import io.github.edmm.plugins.ansible.model.AnsibleFile;
import io.github.edmm.plugins.ansible.model.AnsiblePlay;
import io.github.edmm.plugins.ansible.model.AnsibleTask;
import io.github.edmm.plugins.terraform.TerraformPlugin;
import io.github.edmm.plugins.terraform.aws.TerraformAwsVisitor;
import io.github.edmm.plugins.terraform.model.FileProvisioner;
import io.github.edmm.plugins.terraform.model.RemoteExecProvisioner;
import io.github.edmm.plugins.terraform.model.aws.Ec2;
import org.apache.commons.io.FilenameUtils;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;


public class AnsibleVisitor implements ComponentVisitor, RelationVisitor {

    protected final TransformationContext context;
    protected final Configuration cfg = TemplateHelper.forClasspath(TerraformPlugin.class, "/plugins/ansible");
    protected final Graph<RootComponent, RootRelation> graph;
    private static final Logger logger = LoggerFactory.getLogger(AnsibleVisitor.class);
    private List<AnsiblePlay> plays = new ArrayList<>();

    public AnsibleVisitor(TransformationContext context) {
        this.context = context;
        this.graph = context.getTopologyGraph();
    }


    @Override
    public void visit(RootComponent component) {
        logger.info("Generate a play for component " + component.getName());
        PluginFileAccess fileAccess = context.getSubDirAccess();

        Map<String, String> envVars = collectEnvVars(component);
        //scripts that are executed
        List<AnsibleTask> tasks = prepareTasks(collectOperations(component));
        List<AnsibleFile> files=collectFiles(component);

        logger.info("files: {}",files);


        // host is the compute if exists
        String hosts = component.getNormalizedName();
        Optional<Compute> optionalCompute = TopologyGraphHelper.resolveHostingComputeComponent(context.getTopologyGraph(), component);
        if (optionalCompute.isPresent()) {
            hosts = optionalCompute.get().getNormalizedName();
        }

        AnsiblePlay play = AnsiblePlay.builder()
                .name(component.getName())
                .hosts(hosts)
                .vars(envVars)
                .tasks(tasks)
                .files(files)
                .build();

        plays.add(play);
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("plays", plays);
        String filename = String.format("%s.yml", component.getNormalizedName());

        try {
            fileAccess.write(filename, TemplateHelper.toString(cfg, "playbook_base.yml", templateData));
        } catch (IOException e) {
            logger.error("Failed to write Ansible file", e);
        }
        component.setTransformed(true);
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

    private Map<String, String> collectEnvVars(RootComponent component) {

        Map<String, String> envVars = new HashMap<>();
        String[] blacklist = {"key_name", "public_key"};
        component.getProperties().values().stream()
                .filter(p -> !Arrays.asList(blacklist).contains(p.getName()))
                .filter(p -> p.getValue() != null || !p.isComputed())
                .forEach(p -> {
                    String name = (component.getNormalizedName() + "_" + p.getNormalizedName());
                    envVars.put(name, p.getValue());
                });

        return envVars;
    }

    private List<AnsibleFile> collectFiles(RootComponent component) {

            List<AnsibleFile> fileList=new ArrayList<>();
            for (Artifact artifact : component.getArtifacts()) {
                String filename=FilenameUtils.getName(artifact.getValue());
                String destination = "/opt/" + component.getNormalizedName()+"/";
                fileList.add(new AnsibleFile("./files/"+ filename, destination+filename));
            }
            return fileList;

    }

    // convert operations to tasks
    private List<AnsibleTask> prepareTasks(List<Operation> operations) {
        List<AnsibleTask> taskQueue = new ArrayList<>();
        operations.forEach(operation -> {
            if (!operation.getArtifacts().isEmpty()) {
                String file = operation.getArtifacts().get(0).getValue();
                AnsibleTask task = AnsibleTask.builder()
                        .name(operation.getNormalizedName())
                        .script(file)
                        .build();
                taskQueue.add(task);
            }
        });
        return taskQueue;
    }

    private List<Operation> collectOperations(RootComponent component) {
        List<Operation> operations = new ArrayList<>();
        component.getStandardLifecycle().getCreate().ifPresent(operations::add);
        component.getStandardLifecycle().getConfigure().ifPresent(operations::add);
        component.getStandardLifecycle().getStart().ifPresent(operations::add);
        return operations;
    }


}
