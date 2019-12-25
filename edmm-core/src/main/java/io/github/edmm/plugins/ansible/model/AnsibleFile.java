package io.github.edmm.plugins.ansible.model;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.Map;

@Value
public class AnsibleFile {
    private String src;
    private String target;
}


