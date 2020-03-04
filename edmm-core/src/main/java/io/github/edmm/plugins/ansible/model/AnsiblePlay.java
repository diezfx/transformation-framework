package io.github.edmm.plugins.ansible.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnsiblePlay {
    private String name;
    private String hosts;
    private boolean become;
    private String becomeUser;
    private Map<String, String> vars;
    private List<String> runtimeVars;
    private List<AnsibleTask> tasks;
    private List<AnsibleFile> files;
}
