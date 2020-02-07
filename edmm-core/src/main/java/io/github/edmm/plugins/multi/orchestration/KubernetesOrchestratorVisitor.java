package io.github.edmm.plugins.multi.orchestration;

import com.amazonaws.util.IOUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import freemarker.template.Configuration;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.plugin.TemplateHelper;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.core.transformation.TransformationException;
import io.github.edmm.model.Artifact;
import io.github.edmm.model.Metadata;
import io.github.edmm.model.Property;
import io.github.edmm.model.PropertyBlocks;
import io.github.edmm.model.component.*;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.plugins.kubernetes.model.ConfigMapResource;
import io.github.edmm.plugins.kubernetes.model.KubernetesResource;
import io.github.edmm.plugins.multi.MultiPlugin;
import lombok.SneakyThrows;
import lombok.var;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KubernetesOrchestratorVisitor implements ComponentVisitor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesOrchestratorVisitor.class);
    protected final TransformationContext context;
    protected final Configuration cfg = TemplateHelper.forClasspath(MultiPlugin.class, "/plugins/multi");
    protected final Graph<RootComponent, RootRelation> graph;

    public KubernetesOrchestratorVisitor(TransformationContext context) {
        this.context = context;
        this.graph = context.getTopologyGraph();
    }


    private void createConfigMap(RootComponent component) {
        PluginFileAccess fileAccess = this.context.getSubDirAccess();
        PropertyBlocks reqs = OrchestrationHelper.findAllRequirements(graph, component, logger);

        var config = new ConfigMapResource(component, reqs);
        config.build();
        try {
            String targetDirectory = component.getName();
            fileAccess.write(targetDirectory + "/" + config.getName() + ".yaml", config.toYaml());

        } catch (Exception e) {
            logger.error("Failed to create ConfigMap for stack '{}'", component.getName(), e);
            throw new TransformationException(e);
        }


    }

    @Override
    public void visit(RootComponent component) {

        if (!TopologyGraphHelper.isComponentHostedOnLeaf(graph, component)) {
            return;
        }
        PluginFileAccess fileAccess = this.context.getSubDirAccess();

        createConfigMap(component);

        ProcessBuilder pb = new ProcessBuilder();
        File compDir = new File(fileAccess.getTargetDirectory(), component.getName());
        pb.directory(compDir);


        String label = component.getName().replace("_", "-");
        String registry = "localhost:32000/";


        // docker build &push
        try {
            pb.command("docker", "build", "-t", label + ":latest", ".");
            Process init = pb.start();
            String output = IOUtils.toString(init.getInputStream());
            init.waitFor();
            pb.command("docker", "tag", label + ":latest", registry + label);
            pb.start().waitFor();
            pb.command("docker", "push", registry + label);
            pb.start().waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


        try {
            pb.inheritIO();
            // apply config
            File configYaml = new File(compDir, label + "-config.yaml");
            if (configYaml.exists()) {
                pb.command("microk8s.kubectl", "apply", "-f", configYaml.getAbsolutePath());
                pb.start().waitFor();
            }
            //apply deployment
            File deployYaml = new File(compDir, label + "-deployment.yaml");
            if (deployYaml.exists()) {
                pb.command("microk8s.kubectl", "apply", "-f", deployYaml.getAbsolutePath());
                pb.start().waitFor();
            }

            // apply service
            File serviceYaml = new File(compDir, label + "-service.yaml");
            if (serviceYaml.exists()) {
                pb.command("microk8s.kubectl", "apply", "-f", serviceYaml.getAbsolutePath());
                pb.start().waitFor();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void visit(Compute component) {

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
