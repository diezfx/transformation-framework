package io.github.edmm.plugins.multi;

import freemarker.template.Configuration;
import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.plugin.TemplateHelper;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
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
import lombok.var;
import org.apache.commons.io.FilenameUtils;
import org.checkerframework.checker.nullness.Opt;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class AnsibleVisitor implements ComponentVisitor, RelationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(AnsibleVisitor.class);
    protected final TransformationContext context;
    protected final Configuration cfg = TemplateHelper.forClasspath(AnsibleVisitor.class, "/plugins/ansible");
    protected final Graph<RootComponent, RootRelation> graph;
    private final List<AnsiblePlay> plays = new ArrayList<>();

    public AnsibleVisitor(TransformationContext context) {
        this.context = context;
        this.graph = context.getTopologyGraph();
    }

    @Override
    public void visit(RootComponent component) {

        logger.info("Generate a play for component " + component.getName());
        PluginFileAccess fileAccess = context.getSubDirAccess();
        copyFiles(component);

        Map<String, String> envVars = collectEnvVars(component);
        List<String> envVarsRuntime = collectRuntimeEnvVars(component);
        // scripts that are executed
        List<AnsibleTask> tasks = prepareTasks(collectOperations(component), component);
        List<AnsibleFile> files = collectFiles(component);

        // host is the compute if exists

        Optional<Compute> optionalCompute = TopologyGraphHelper
                .resolveHostingComputeComponent(context.getTopologyGraph(), component);

        if (!optionalCompute.isPresent()) {
            throw new IllegalStateException(String.format("The component %s could doesn't have a hosting compute", component.getName()));


        }
        String hosts = optionalCompute.get().getNormalizedName();
        Optional<String> privKeyPath = optionalCompute.get().getPrivateKeyPath();

        AnsiblePlay play = AnsiblePlay.builder().name(component.getName()).hosts(hosts).vars(envVars).runtimeVars(envVarsRuntime).tasks(tasks)
                .files(files).privKeyFile(privKeyPath.get()).build();

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


    private boolean matchesBlacklist(Map.Entry<String, Property> prop) {
        String[] blacklist = {"*key_name*", "*public_key*", "hostname"};
        logger.info(prop.getKey());
        for (var blacklistVal : blacklist) {
            if (FilenameUtils.wildcardMatch(prop.getKey(), blacklistVal)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> collectEnvVars(RootComponent component) {
        Map<String, String> envVars = new HashMap<>();
        var allProps = TopologyGraphHelper.findAllProperties(graph, component);

        for (var prop : allProps.entrySet()) {
            if (matchesBlacklist(prop)) {
                continue;
            }


            if (prop.getValue().isComputed() || prop.getValue().getValue() == null || prop.getValue().getValue().startsWith("$")) {
                continue;
            }
            envVars.put(prop.getKey().toUpperCase(), prop.getValue().getValue());
        }

        return envVars;
    }

    private List<String> collectRuntimeEnvVars(RootComponent component) {
        //runtime vars
        List<String> envVars = new ArrayList<>();
        var allProps = TopologyGraphHelper.findAllProperties(graph, component);

        for (var prop : allProps.entrySet()) {

            if (matchesBlacklist(prop)) {
                continue;
            }
            if (prop.getValue().isComputed() || prop.getValue().getValue() == null || prop.getValue().getValue().startsWith("$")) {
                envVars.add(prop.getKey().toUpperCase());
            }
        }
        return envVars;
    }

    private List<AnsibleFile> collectFiles(RootComponent component) {

        List<AnsibleFile> fileList = new ArrayList<>();
        for (Artifact artifact : component.getArtifacts()) {
            String basename = FilenameUtils.getName(artifact.getValue());
            String newPath = "./files/" + basename;
            String destination = "/opt/";
            fileList.add(new AnsibleFile(newPath, destination + basename));
        }
        return fileList;

    }

    // convert operations to tasks
    private List<AnsibleTask> prepareTasks(List<Artifact> operations, RootComponent component) {
        List<AnsibleTask> taskQueue = new ArrayList<>();
        operations.forEach(operation -> {
            String basename = FilenameUtils.getName(operation.getValue());
            String newPath = "./files/" + basename;
            Map<String, String> args = new HashMap<>();
            args.put("chdir", "/opt/");
            AnsibleTask task = AnsibleTask.builder().name(operation.getNormalizedName()).script(newPath).args(args).build();
            taskQueue.add(task);

        });
        return taskQueue;
    }

    private void copyFiles(RootComponent comp) {
        PluginFileAccess fileAccess = context.getFileAccess();
        for (Artifact artifact : comp.getArtifacts()) {
            try {
                // get basename
                String basename = FilenameUtils.getName(artifact.getValue());
                String newPath = "./files/" + basename;
                fileAccess.copy(artifact.getValue(), comp.getNormalizedName() + "/" + newPath);
            } catch (IOException e) {
                logger.warn("Failed to copy file '{}'", artifact.getValue());
            }

        }
        List<Artifact> operations = collectOperations(comp);

        for (Artifact artifact : operations) {
            try {
                String basename = FilenameUtils.getName(artifact.getValue());
                String newPath = "./files/" + basename;
                fileAccess.copy(artifact.getValue(), comp.getNormalizedName() + "/" + newPath);
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
