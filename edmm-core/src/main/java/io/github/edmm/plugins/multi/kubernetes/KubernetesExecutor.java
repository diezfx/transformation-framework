package io.github.edmm.plugins.multi.kubernetes;

import io.github.edmm.core.transformation.TransformationException;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.plugins.kubernetes.model.ConfigMapResource;
import io.github.edmm.plugins.multi.orchestration.ExecutionCompInfo;
import io.github.edmm.plugins.multi.orchestration.ExecutionContext;
import io.github.edmm.plugins.multi.orchestration.GroupExecutor;
import io.github.edmm.utils.Consts;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1Deployment;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;
import lombok.var;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KubernetesExecutor implements GroupExecutor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesExecutor.class);
    protected final ExecutionContext orchContext;

    public KubernetesExecutor(ExecutionContext context) {
        this.orchContext = context;
    }

    private static V1ConfigMap createConfigMap(RootComponent component, Map<String, Property> computedProps, File dir) {

        var config = new ConfigMapResource(component, computedProps);
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

    public static Optional<V1Service> deployService(RootComponent component, File dir, CoreV1Api api) {
        V1Service result = null;
        try {
            // apply deployment
            File serviceYaml = new File(dir, component.getLabel() + "-service.yaml");
            V1Service service = (V1Service) Yaml.load(serviceYaml);
            // this throws an exception if already exists
            try {
                result = api.createNamespacedService(service.getMetadata().getNamespace(), service, "true", null, null);
            } catch (ApiException e) {
                api.deleteNamespacedService(service.getMetadata().getName(), service.getMetadata().getNamespace(), null,
                        null, null, null, null, null);
                result = api.createNamespacedService(service.getMetadata().getNamespace(), service, "true", null, null);
            }
        } catch (IOException | ApiException e) {
            e.printStackTrace();
        }
        return Optional.of(result);

    }

    public static void deployDeployment(RootComponent component, File dir, AppsV1Api api) {
        try {
            // apply deployment
            File deployYaml = new File(dir, component.getLabel() + "-deployment.yaml");
            V1Deployment depl = (V1Deployment) Yaml.load(deployYaml);
            // this throws an exception if already exists
            try {
                api.createNamespacedDeployment(depl.getMetadata().getNamespace(), depl, "true", null, null);
            } catch (ApiException e) {
                api.deleteNamespacedDeployment(depl.getMetadata().getName(), depl.getMetadata().getNamespace(), null,
                        null, null, null, null, null);
                api.createNamespacedDeployment(depl.getMetadata().getNamespace(), depl, "true", null, null);
            }
        } catch (IOException | ApiException e) {
            logger.info("this happemns");
            e.printStackTrace();
        }

    }

    public static void deployConfigMap(RootComponent component, Map<String, Property> props, File dir, CoreV1Api api) {
        try {
            V1ConfigMap config = createConfigMap(component, props, dir);
            // this throws an exception if already exists
            try {
                api.createNamespacedConfigMap(config.getMetadata().getNamespace(), config, "true", null, null);
            } catch (ApiException e) {
                api.deleteNamespacedConfigMap(config.getMetadata().getName(), config.getMetadata().getNamespace(), null,
                        null, null, null, null, null);
                api.createNamespacedConfigMap(config.getMetadata().getNamespace(), config, null, null, null);
            }

        } catch (ApiException e) {
            e.printStackTrace();
        }

    }

    public void execute(List<ExecutionCompInfo> deployInfos) {
        File fileAccess = this.orchContext.getDirAccess();
        for (var info : deployInfos) {
            File compDir = new File(fileAccess, info.getComponent().getName());
            if (!compDir.exists()) {
                continue;
            }

            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(compDir);
            pb.inheritIO();

            // hardcoded registry for now
            String registry = "localhost:32000/";
            // docker build &push
            try {
                pb.command("docker", "build", "-t", info.getComponent().getLabel() + ":latest", ".");
                Process init = pb.start();
                init.waitFor();
                pb.command("docker", "tag", info.getComponent().getLabel() + ":latest",
                        registry + info.getComponent().getLabel());
                pb.start().waitFor();
                pb.command("docker", "push", registry + info.getComponent().getLabel());
                pb.start().waitFor();
            } catch (IOException | InterruptedException e) {
                logger.error("could not deploy comp: {}", info.getComponent().getName());
                e.printStackTrace();
            }

            try {
                ApiClient client = Config.defaultClient();
                AppsV1Api deploymentApi = new AppsV1Api();
                deploymentApi.setApiClient(client);
                CoreV1Api api = new CoreV1Api();
                api.setApiClient(client);

                // contains the runtime properties
                deployConfigMap(info.getComponent(), info.getProperties(), compDir, api);

                // deploy everything
                logger.info("deployed configMap for {}", info.getComponent().getName());
                deployDeployment(info.getComponent(), compDir, deploymentApi);
                logger.info("deployed deployment for {}", info.getComponent().getName());
                Optional<V1Service> service = deployService(info.getComponent(), compDir, api);
                logger.info("deployed service for {}", info.getComponent().getName());

                // read output
                for (var port : service.get().getSpec().getPorts()) {
                    logger.info("the ’public’ nodeport is: {}", port.getNodePort().toString());

                }
                logger.info("the clusterIP is: {}", service.get().getSpec().getClusterIP());
                info.getComponent().addProperty("hostname", service.get().getSpec().getClusterIP());

            } catch (IOException e) {
                logger.error("could not deploy comp: {}", info.getComponent().getName());
                e.printStackTrace();

            }

            try {
                logger.info("wait for component to start");
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
