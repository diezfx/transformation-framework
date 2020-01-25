package io.github.edmm.plugins.azure.model.resource.compute.virtualmachines;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.edmm.plugins.azure.model.resource.network.networkinterfaces.NetworkInterface;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NetworkProfile {
    private List<NetworkInterface> networkInterfaces;
}
