package io.github.edmm.plugins.heat.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;

@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Resource {

    @JsonIgnore
    @EqualsAndHashCode.Include
    private String name;
    private String type;
    private List<String> dependsOn;
    private Map<String, PropertyAssignment> properties;

    public void addPropertyAssignment(String name, PropertyAssignment property) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(name, property);
    }

    public void addDependsOn(String... deps) {
        if (dependsOn == null) {
            dependsOn = new ArrayList<>();
        }
        dependsOn.addAll(Arrays.asList(deps));
    }
}
