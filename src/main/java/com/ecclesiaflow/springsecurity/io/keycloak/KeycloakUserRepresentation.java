package com.ecclesiaflow.springsecurity.io.keycloak;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Keycloak user representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakUserRepresentation {
    private String username;
    private String email;
    private boolean emailVerified;
    private boolean enabled;
    private List<KeycloakCredential> credentials;
}
