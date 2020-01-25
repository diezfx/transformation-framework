package io.github.edmm.plugins.heat.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

@Value
public class PropertyGetAttr implements PropertyAssignment {

    @JsonProperty("get_attr")
    private List<Object> values;
}
