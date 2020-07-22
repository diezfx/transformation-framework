package io.github.edmm.plugins.multi.model;

public class ComponentResources {
    public String getName() {
        return name;
    }

    private final String name;

    // maybe used later
    // List<String> expectedRuntimeProps;
    // List<String> runtimePropsOutput;

    public ComponentResources(String name) {
        this.name = name;
    }

    public void addInputProp(String p) {
        // expectedRuntimeProps.add(p);
    }

}
