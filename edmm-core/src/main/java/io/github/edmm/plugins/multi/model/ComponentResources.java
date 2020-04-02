package io.github.edmm.plugins.multi.model;

import java.util.ArrayList;
import java.util.List;

public class ComponentResources {
    public String getName() {
        return name;
    }


    private final String name;
    List<String> expectedRuntimeProps;

    //maybe used later
    //List<String> runtimePropsOutput;

    public ComponentResources(String name) {
        this.name = name;
        this.expectedRuntimeProps = new ArrayList<>();
        //this.runtimePropsOutput= new ArrayList<>();
    }

    public ComponentResources(String name, List<String> expectedRuntimeProps) {
        this.name = name;
        this.expectedRuntimeProps = expectedRuntimeProps;

    }


    public void addInputProp(String p) {
        expectedRuntimeProps.add(p);
    }

}
