package io.github.edmm.model;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.*;
import java.util.List;

@Getter
@Builder
public class PluginSupportResult {

    @NotBlank
    private final String id;

    @NotBlank
    private final String name;

    @Min(0)
    @Max(1)
    @PositiveOrZero
    private final Double supports;

    @NotNull
    private final List<String> unsupportedComponents;
}
