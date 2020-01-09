package io.github.edmm.web.model;

import javax.validation.constraints.NotEmpty;

import io.github.edmm.web.model.support.Platform;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class TransformationModel {

    @NotEmpty
    @Platform
    @Schema(description = "The name of the transformation target", required = true)
    private String target;

    @NotEmpty
    @Schema(description = "The EDMM input as Base64 encoded string", required = true)
    private String input;
}
