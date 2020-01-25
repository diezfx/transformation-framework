package io.github.edmm.plugins.terraform.model;

import lombok.Value;

import java.util.List;

@Value
public class RemoteExecProvisioner {

    private List<String> scripts;
}
