package io.github.edmm.plugins.cloudify.model.azure;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class Operation {
    @Setter
    @Getter
    private List<Operation> previous;

    @Setter
    @Getter
    private String name;

    @Setter
    @Getter
    private String source;

    @Getter
    private Script script;

    public Operation(String name, String source, List<Operation> previous) {
        this.name = name;
        this.source = source;
        this.previous = previous;
    }

    public void setScript(String name, String path) {
        this.script = Script.builder().name(name).path(path).build();
    }
}
