package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.business.domain.password.PasswordManagement;
import com.ecclesiaflow.springsecurity.web.api.PasswordManagementApi;
import com.ecclesiaflow.springsecurity.web.delegate.PasswordManagementDelegate;
import com.ecclesiaflow.springsecurity.web.model.AddCredentialsRequest;
import com.ecclesiaflow.springsecurity.web.model.PasswordManagementResponse;
import com.ecclesiaflow.springsecurity.web.model.SetupPasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for password management operations.
 * Delegates to Keycloak Admin API for initial password setup.
 *
 * @author EcclesiaFlow Team
 * @since 2.0.0
 */
@RestController
@RequiredArgsConstructor
public class PasswordController implements PasswordManagementApi {

    private final PasswordManagementDelegate passwordManagementDelegate;

    @Override
    public ResponseEntity<PasswordManagementResponse> _authSetInitialPassword(
            String xSetupToken, SetupPasswordRequest setupPasswordRequest) {
        PasswordManagement passwordManagement = new PasswordManagement(
                setupPasswordRequest.getPassword(), xSetupToken);
        return passwordManagementDelegate.setupPassword(passwordManagement.xSetupToken(), passwordManagement.password());
    }

    @Override
    public ResponseEntity<PasswordManagementResponse> _authAddLocalCredentials(
            AddCredentialsRequest addCredentialsRequest) {
        return passwordManagementDelegate.addLocalCredentials(addCredentialsRequest.getPassword());
    }
}
