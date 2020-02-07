package io.github.edmm.plugins.kubernetes.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import io.github.edmm.core.transformation.TransformationException;
import io.github.edmm.docker.Container;
import io.github.edmm.docker.PortMapping;
import io.github.edmm.model.Property;
import io.github.edmm.model.PropertyBlocks;
import io.github.edmm.model.component.RootComponent;
import lombok.var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public final class ConfigMapResource implements KubernetesResource {

    private static final Logger logger = LoggerFactory.getLogger(ServiceResource.class);
    private final RootComponent component;
    private ConfigMap configMap;
    private final PropertyBlocks props;

    public ConfigMapResource(RootComponent component, PropertyBlocks props) {
        this.component = component;
        this.props = props;
    }

    @Override
    public void build() {

        var configMapBuilder = new ConfigMapBuilder().withNewMetadata()
                .withName(getName())
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
        try {
            return SerializationUtils.dumpAsYaml(configMap);
        } catch (JsonProcessingException e) {
            logger.error("Failed to dump YAML", e);
            throw new TransformationException(e);
        }
    }

    @Override
    public String getName() {
        String label = component.getName().replace("_", "-");
        return label + "-config";
    }
}
