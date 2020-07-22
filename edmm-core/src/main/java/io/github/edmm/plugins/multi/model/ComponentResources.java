package io.github.edmm.plugins.multi.model;

public class ComponentResources {
    private final String name;

    public ComponentResources(String name) {
        this.name = name;
    }

    // maybe used later
    // List<String> expectedRuntimeProps;
    // List<String> runtimePropsOutput;

    public String getName() {
        return name;
    }

    public void addInputProp(String p) {
        // expectedRuntimeProps.add(p);
    }

}
