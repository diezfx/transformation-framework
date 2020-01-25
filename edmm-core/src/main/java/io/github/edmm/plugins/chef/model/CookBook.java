package io.github.edmm.plugins.chef.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CookBook {
    private String name;
    private String path;
    private List<ShellRecipe> shellRecipes;
}
