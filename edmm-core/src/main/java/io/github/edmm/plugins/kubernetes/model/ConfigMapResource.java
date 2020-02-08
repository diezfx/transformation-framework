package io.github.edmm.plugins.kubernetes.model;


import io.github.edmm.core.transformation.TransformationException;
import io.github.edmm.model.PropertyBlocks;
import io.github.edmm.model.component.RootComponent;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ConfigMapBuilder;
import io.kubernetes.client.util.Yaml;
import lombok.var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class ConfigMapResource implements KubernetesResource {

    private static final Logger logger = LoggerFactory.getLogger(ServiceResource.class);
    private final RootComponent component;
    private V1ConfigMap configMap;
    private final PropertyBlocks props;
    private final String namespace = "default";

    public ConfigMapResource(RootComponent component, PropertyBlocks props) {
        this.component = component;
        this.props = props;
    }

    @Override
    public void build() {

        var configMapBuilder = new V1ConfigMapBuilder().withNewMetadata()
                .withName(getName())
                .withNamespace(namespace)
                .endMetadata();

        for (var prop : props.flattenBlocks().entrySet()) {
            configMapBuilder = configMapBuilder.addToData(prop.getKey(), prop.getValue().getValue());

        }
        configMap = configMapBuilder.build();
    }

    @Override
    public String toYaml() {
        if (configMap == null) {
            throw new TransformationException("Resource not yet built, call build() first");
        }

        return Yaml.dump(configMap);
    }

    public V1ConfigMap getConfigMap() {
        return configMap;
    }

    @Override
    public String getName() {
        String label = component.getName().replace("_", "-");
        return label + "-config";
    }
}
