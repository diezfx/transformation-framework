package io.github.edmm.plugins.ansible.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AnsibleTask {
    private String name;
    private String script;
    private Map<String, String> args;
}
