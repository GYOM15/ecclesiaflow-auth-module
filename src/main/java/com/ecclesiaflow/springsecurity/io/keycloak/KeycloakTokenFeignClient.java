package com.ecclesiaflow.springsecurity.io.keycloak;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "keycloak-token", url = "${keycloak.admin.server-url:http://localhost:8180}")
public interface KeycloakTokenFeignClient {

    @PostMapping(
            value = "/realms/{realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    KeycloakTokenResponse getToken(
            @PathVariable String realm,
            @RequestBody MultiValueMap<String, String> form
    );
}
