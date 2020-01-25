package io.github.edmm.model;

import lombok.Data;
import lombok.NonNull;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
public class ComponentInterface {

    @NonNull Map<String, Property> provides;
    @NonNull Map<String, Property> requires;

    @NonNull Map<String, Property> hostingCompsProvides;
    @NonNull Map<String, Property> hostingCompsRequires;


    public Map<String, Property> getAllProvided() {
        Map<String, Property> properties = new HashMap<>(provides);
        hostingCompsProvides.forEach(properties::putIfAbsent);
        return properties;
    }

    public Map<String, Property> getAllRequired() {
        Map<String, Property> properties = new HashMap<>(requires);
        hostingCompsRequires.forEach(properties::putIfAbsent);
        return properties;
    }

}
