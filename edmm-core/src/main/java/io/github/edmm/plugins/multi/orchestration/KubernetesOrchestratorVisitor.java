package io.github.edmm.plugins.multi.orchestration;

import io.github.edmm.model.Property;
import io.github.edmm.utils.Consts;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.util.Config;
import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.core.transformation.TransformationException;
import io.github.edmm.model.component.*;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.plugins.kubernetes.model.ConfigMapResource;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.models.V1Deployment;
import io.kubernetes.client.util.Yaml;
import lombok.var;
import org.apache.commons.io.FileUtils;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KubernetesOrchestratorVisitor implements ComponentVisitor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesOrchestratorVisitor.class);
    protected final TransformationContext context;
    protected final Graph<RootComponent, RootRelation> graph;

    public KubernetesOrchestratorVisitor(TransformationContext context) {
        this.context = context;
        this.graph = context.getTopologyGraph();
    }


    private V1ConfigMap createConfigMap(RootComponent component, File dir) {
        Map<String, Property> allProps = TopologyGraphHelper.findAllProperties(graph, component);
        Map<String, Property> computedProps = new HashMap<>();
        for (var prop : allProps.entrySet()) {
            if (prop.getValue().isComputed() || prop.getValue().getValue() == null || prop.getValue().getValue().startsWith("$")) {
                computedProps.put(prop.getKey(), prop.getValue());
            }
        }

        var resolvedComputedProps = TopologyGraphHelper.resolveAllPropertyReferences(graph, component, computedProps);


        var config = new ConfigMapResource(component, resolvedComputedProps);
        config.build();
        try {
            File serviceYaml = new File(dir, component.getLabel() + "-config.yaml");
            FileUtils.writeStringToFile(serviceYaml, config.toYaml() + Consts.NL, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.error("Failed to create ConfigMap for stack '{}'", component.getName(), e);
            throw new TransformationException(e);
        }
        return config.getConfigMap();
    }


    public Optional<V1Service> deployService(RootComponent component, File dir, CoreV1Api api) {
        V1Service result = null;
        try {
            //apply deployment
            File serviceYaml = new File(dir, component.getLabel() + "-service.yaml");
            V1Service service = (V1Service) Yaml.load(serviceYaml);
            // this throws an exception if already exists
            try {
                result = api.createNamespacedService(service.getMetadata().getNamespace(), service, true, null, null);
            } catch (ApiException e) {
                api.deleteNamespacedService(service.getMetadata().getName(), service.getMetadata().getNamespace(), null, null, null, null, null, null);
                result = api.createNamespacedService(service.getMetadata().getNamespace(), service, true, null, null);
            }
        } catch (IOException | ApiException e) {
            e.printStackTrace();
        }
        return Optional.of(result);

    }

    public void deployDeployment(RootComponent component, File dir, AppsV1Api api) {
        try {
            //apply deployment
            File deployYaml = new File(dir, component.getLabel() + "-deployment.yaml");
            V1Deployment depl = (V1Deployment) Yaml.load(deployYaml);
            // this throws an exception if already exists
            try {
                api.createNamespacedDeployment(depl.getMetadata().getNamespace(), depl, true, null, null);
            } catch (ApiException e) {
                api.deleteNamespacedDeployment(depl.getMetadata().getName(), depl.getMetadata().getNamespace(), null, null, null, null, null, null);
                api.createNamespacedDeployment(depl.getMetadata().getNamespace(), depl, true, null, null);
            }
        } catch (IOException | ApiException e) {
            e.printStackTrace();
        }

    }

    public void deployConfigMap(RootComponent component, File dir, CoreV1Api api) {
        try {
            V1ConfigMap config = createConfigMap(component, dir);
            // this throws an exception if already exists
            try {
                api.createNamespacedConfigMap(config.getMetadata().getNamespace(), config, true, null, null);
            } catch (ApiException e) {
                api.deleteNamespacedConfigMap(config.getMetadata().getName(), config.getMetadata().getNamespace(), null, null, null, null, null, null);
                api.createNamespacedConfigMap(config.getMetadata().getNamespace(), config, null, null, null);
            }


        } catch (ApiException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void visit(RootComponent component) {

        if (!TopologyGraphHelper.isComponentHostedOnLeaf(graph, component)) {
            return;
        }
        PluginFileAccess fileAccess = this.context.getSubDirAccess();

        ProcessBuilder pb = new ProcessBuilder();
        File compDir = new File(fileAccess.getTargetDirectory(), component.getName());
        pb.directory(compDir);
        pb.inheritIO();

        // hardcoded registry for now
        String registry = "localhost:32000/";


        // docker build &push
        try {
            pb.command("docker", "build", "-t", component.getLabel() + ":latest", ".");
            Process init = pb.start();
            init.waitFor();
            pb.command("docker", "tag", component.getLabel() + ":latest", registry + component.getLabel());
            pb.start().waitFor();
            pb.command("docker", "push", registry + component.getLabel());
            pb.start().waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        try {
            ApiClient client = Config.defaultClient();
            AppsV1Api deploymentApi = new AppsV1Api();
            deploymentApi.setApiClient(client);
            CoreV1Api api = new CoreV1Api();
            api.setApiClient(client);

            // contains the runtime properties
            deployConfigMap(component, compDir, api);

            // deploy everything
            logger.info("deployed configMap for {}", component.getName());
            deployDeployment(component, compDir, deploymentApi);
            logger.info("deployed deployment for {}", component.getName());
            Optional<V1Service> service = deployService(component, compDir, api);
            logger.info("deployed service for {}", component.getName());

            // read output
            for (var port : service.get().getSpec().getPorts()) {
                logger.info("the ’public’ nodeport is: {}", port.getNodePort().toString());

            }
            logger.info("the clusterIP is: {}", service.get().getSpec().getClusterIP());
            component.addProperty("hostname", service.get().getSpec().getClusterIP());


        } catch (IOException e) {
            logger.error("could not deploy comp: {}", component.getName());
            e.printStackTrace();

        }

        try {
            logger.info("wait for component to start");
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void visit(Compute component) {
        visit((RootComponent) component);
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
