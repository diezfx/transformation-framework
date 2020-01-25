package io.github.edmm.plugins.azure.model.resource.compute.virtualmachines.extensions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomScriptSettings {
    private List<String> fileUrls;
    private String commandToExecute;
}
