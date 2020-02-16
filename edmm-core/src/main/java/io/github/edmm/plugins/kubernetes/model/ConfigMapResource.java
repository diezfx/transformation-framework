package io.github.edmm.plugins.kubernetes.model;


import io.github.edmm.core.transformation.TransformationException;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.RootComponent;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ConfigMapBuilder;
import io.kubernetes.client.util.Yaml;
import lombok.var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;


public final class ConfigMapResource implements KubernetesResource {

    private static final Logger logger = LoggerFactory.getLogger(ServiceResource.class);
    private final RootComponent component;
    private V1ConfigMap configMap;
    private final String namespace = "default";
    private final Map<String, Property> props;

    String[] blacklist = {"key_name", "public_key", "hostname"};

    public ConfigMapResource(RootComponent component, Map<String, Property> props) {
        this.component = component;
        this.props = props;
    }

    @Override
    public void build() {

        var configMapBuilder = new V1ConfigMapBuilder().withNewMetadata()
                .withName(getName())
                .withNamespace(namespace)
                .endMetadata();

        for (var prop : props.entrySet()) {

            if (Arrays.asList(blacklist).contains(prop.getKey())) {
                continue;
            }
            logger.info(prop.getKey());
            configMapBuilder = configMapBuilder.addToData(prop.getKey().toUpperCase(), prop.getValue().getValue().toUpperCase());

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
