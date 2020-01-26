package io.github.edmm.model;

import lombok.Data;
import lombok.NonNull;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
public class RequiredProperties {


    @NonNull Map<String,Map<String,Property>> requires;

    public Map<String,Property> getHostingRequirements(){
        return requires.get("host");
    }

}
