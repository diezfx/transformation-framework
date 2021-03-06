package io.github.edmm.plugins.chef.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PolicyFile {
    private String name;
    private String runningOrder;
    private List<CookBook> cookbooks;
}
