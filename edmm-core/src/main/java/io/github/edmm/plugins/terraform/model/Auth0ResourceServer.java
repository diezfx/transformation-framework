package io.github.edmm.plugins.terraform.model;

import io.github.edmm.utils.Consts;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Auth0ResourceServer {

    private String name;
    private String domain = Consts.EMPTY;
    private String clientId = Consts.EMPTY;
    private String clientSecret = Consts.EMPTY;
    private String identifier = Consts.EMPTY;
    private List<String> scopes = new ArrayList<>();
}
