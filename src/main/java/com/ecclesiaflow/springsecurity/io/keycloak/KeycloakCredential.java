package com.ecclesiaflow.springsecurity.io.keycloak;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Write-only DTO for Keycloak credential representation.
 * No toString — {@code value} contains the raw password.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeycloakCredential {
    private String type;
    private String value;
    private Boolean temporary;
}
