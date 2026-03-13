package com.ecclesiaflow.springsecurity.io.keycloak;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Write-only DTO for Keycloak Admin API user representation.
 * <p>
 * Uses {@link Boolean} wrappers so that unset fields remain {@code null}
 * and are excluded by {@link JsonInclude.Include#NON_NULL}, preventing
 * partial updates from accidentally resetting enabled/emailVerified.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeycloakUserRepresentation {
    private String username;
    private String email;
    private Boolean emailVerified;
    private Boolean enabled;
    private List<KeycloakCredential> credentials;
}
