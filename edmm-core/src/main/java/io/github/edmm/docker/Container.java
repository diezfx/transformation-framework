package io.github.edmm.docker;

import io.github.edmm.model.component.RootComponent;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public final class Container {

    private List<RootComponent> components = new ArrayList<>();

    //the variables that will only be known at runtime
    private List<String> envVarsRuntime = new ArrayList<>();

    private String baseImage;
    private Map<String, String> envVars = new HashMap<>();
    private List<FileMapping> artifacts = new ArrayList<>();
    private List<PortMapping> ports = new ArrayList<>();
    private List<FileMapping> operations = new ArrayList<>();
    private List<FileMapping> startOperations = new ArrayList<>();

    public Container(@NonNull Container stack) {
        this.components = new ArrayList<>(stack.components);
        this.baseImage = stack.baseImage;
        this.envVars = new HashMap<>(stack.envVars);
        this.envVarsRuntime = new ArrayList<>(stack.envVarsRuntime);
        this.artifacts = new ArrayList<>(stack.artifacts);
        this.ports = new ArrayList<>(stack.ports);
        this.operations = new ArrayList<>(stack.operations);
        this.startOperations = new ArrayList<>(stack.startOperations);
    }

    public String getName() {
        return components.get(components.size() - 1).getName();
    }

    public RootComponent getTop() { return components.get(components.size() - 1); }

    public String getLabel() {
        return getName().replace("_", "-");
    }

    public String getServiceName() {
        return getLabel() + "-service";
    }

    public String getDeploymentName() {
        return getLabel() + "-deployment";
    }

    public String getConfigMapName() {
        return getLabel() + "-config";
    }

    public void addComponent(RootComponent component) {
        components.add(component);
    }

    public void addEnvVar(String name, String value) {
        envVars.put(name, value);
    }

    public void addEnvVarRuntime(String name) {
        envVarsRuntime.add(name);
    }

    public void addArtifact(FileMapping mapping) {
        artifacts.add(mapping);
    }

    public void addPort(PortMapping mapping) {
        ports.add(mapping);
    }

    public void addOperation(FileMapping mapping) {
        operations.add(mapping);
    }

    public void addStartOperation(FileMapping mapping) {
        startOperations.add(mapping);
    }

    public boolean hasComponent(RootComponent component) {
        return components.contains(component);
    }
}
