package io.github.edmm.web.model;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.*;
import java.util.List;

@Getter
@Builder
public final class PluginSupportResult {

    @NotBlank
    private String id;

    @NotBlank
    private String name;

    @Min(0)
    @Max(1)
    @PositiveOrZero
    private Double supports;

    @NotNull
    private List<String> unsupportedComponents;
}
