package com.ecclesiaflow.springsecurity.web.delegate;

import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.web.constants.Messages;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import com.ecclesiaflow.springsecurity.web.model.PasswordManagementResponse;
import com.ecclesiaflow.springsecurity.web.model.SetupPasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Delegate for password management - Delegate pattern with OpenAPI Generator.
 * <p>
 * This delegate contains all business logic for password management,
 * separating responsibilities between the controller (HTTP handling) and application logic.
 * </p>
 *
 * 
 * <p><strong>Responsibilities:</strong></p>
 * <ul>
 *   <li>Initial password setup after email confirmation</li>
 *   <li>Extraction and validation of temporary token from X-Setup-Token header</li>
 *   <li>Keycloak user creation with the password</li>
 *   <li>Notification of the Members module about account activation</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
public class PasswordManagementDelegate {

    private final PasswordService passwordService;

    public ResponseEntity<PasswordManagementResponse> setupPassword(
            String xSetupToken, String password) {

        if (xSetupToken == null || xSetupToken.isBlank()) {
            throw new InvalidRequestException("Setup token is required");
        }

        Optional<UserTokens> tokens = passwordService.setupPassword(xSetupToken, password);

        PasswordManagementResponse response = new PasswordManagementResponse()
                .message(Messages.PASSWORD_SETUP_SUCCESS);

        tokens.ifPresent(t -> response
                .accessToken(t.accessToken())
                .refreshToken(t.refreshToken())
                .expiresIn(t.expiresIn()));

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<PasswordManagementResponse> addLocalCredentials(String password) {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                .getAuthentication().getCredentials();
        String keycloakUserId = jwt.getSubject();

        passwordService.addLocalCredentials(keycloakUserId, password);

        PasswordManagementResponse response = new PasswordManagementResponse()
                .message("Local password added successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
