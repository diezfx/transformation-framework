package io.github.edmm.plugins.azure.model.resource.network.publicipaddresses;

import io.github.edmm.plugins.azure.model.resource.Properties;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PublicIpAddressProperties extends Properties {
    private String publicIpAllocationMethod;
    private Map<String, String> dnsSettings;
}
