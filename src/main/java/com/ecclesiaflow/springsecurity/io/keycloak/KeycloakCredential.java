package com.ecclesiaflow.springsecurity.io.keycloak;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Keycloak credential representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakCredential {
    private String type;
    private String value;
    private boolean temporary;
}
