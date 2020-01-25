package io.github.edmm.plugins.chef.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PolicyFile {
    private String name;
    private String runningOrder;
    private List<CookBook> cookbooks;
}
