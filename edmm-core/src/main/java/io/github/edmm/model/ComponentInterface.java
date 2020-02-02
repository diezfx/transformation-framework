package io.github.edmm.model;

import lombok.Data;
import lombok.NonNull;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
public class ComponentInterface {

    @NonNull PropertyBlocks capabilities;
    @NonNull PropertyBlocks requirements;


    @NonNull PropertyBlocks HostCapabilities;


}



