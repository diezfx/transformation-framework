package io.github.edmm.plugins.multi.orchestration;

import io.github.edmm.model.Property;
import io.github.edmm.model.component.RootComponent;
import lombok.AllArgsConstructor;
import lombok.Data;


import java.util.Map;

@Data
@AllArgsConstructor
public class DeploymentModelInfo {

    RootComponent component;
    Map<String, Property> properties;

}