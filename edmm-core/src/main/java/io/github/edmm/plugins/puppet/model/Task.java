package io.github.edmm.plugins.puppet.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Task {
    private String name;
    private String scriptFileName;
    private List<String> envVars;
}
