package com.ecclesiaflow.springsecurity.io.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Keycloak token endpoint response.
 * Used for both client_credentials (admin) and password (Direct Grant) flows.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakTokenResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("expires_in")
        int expiresIn,

        @JsonProperty("refresh_expires_in")
        int refreshExpiresIn,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("scope")
        String scope
) {

    @Override
    public String toString() {
        return "KeycloakTokenResponse[accessToken=****, refreshToken=****, expiresIn=" + expiresIn
                + ", tokenType=" + tokenType + ", scope=" + scope + "]";
    }
}
