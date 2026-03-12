package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.exceptions.CompensationFailedException;
import com.ecclesiaflow.springsecurity.business.exceptions.InvalidTokenException;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.business.services.SetupTokenService;
import com.ecclesiaflow.springsecurity.io.keycloak.KeycloakAdminClient;
import com.ecclesiaflow.springsecurity.io.keycloak.KeycloakTokenResponse;
import com.ecclesiaflow.springsecurity.web.constants.Messages;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final SetupTokenService setupTokenService;
    private final KeycloakAdminClient keycloakAdminClient;
    private final MembersClient membersClient;

    @Override
    @Transactional
    public Optional<UserTokens> setupPassword(String setupToken, String password) {
        SetupToken token;
        String keycloakUserId = null;
        try {
            token = setupTokenService.validate(
                    setupToken, SetupToken.TokenPurpose.PASSWORD_SETUP);

            boolean isNotConfirmed = membersClient.isEmailNotConfirmed(token.getEmail());
            if (isNotConfirmed) {
                throw new InvalidRequestException(Messages.PASSWORD_SETUP_ERROR);
            }

            keycloakUserId = keycloakAdminClient.createUser(
                    token.getEmail(),
                    password,
                    true);

            membersClient.notifyAccountActivated(token.getMemberId(), keycloakUserId);

            setupTokenService.deleteToken(token);

        } catch (InvalidTokenException e) {
            throw new InvalidRequestException(Messages.PASSWORD_SETUP_ERROR);
        } catch (KeycloakAdminClient.KeycloakException e) {
            throw new InvalidRequestException(Messages.PASSWORD_SETUP_ERROR);
        } catch (RuntimeException e) {
            if (keycloakUserId != null) {
                compensateKeycloakUser(keycloakUserId);
            }
            throw new InvalidRequestException(Messages.PASSWORD_SETUP_ERROR);
        }

        // Direct Grant: best-effort auto-login after successful setup.
        // Outside compensation scope — setup already succeeded above.
        // If this fails, frontend falls back to standard Keycloak login.
        try {
            KeycloakTokenResponse tokenResponse = keycloakAdminClient.authenticateUser(
                    token.getEmail(), password);
            return Optional.of(new UserTokens(
                    tokenResponse.accessToken(),
                    tokenResponse.refreshToken(),
                    tokenResponse.expiresIn()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void compensateKeycloakUser(String keycloakUserId) {
        try {
            keycloakAdminClient.deleteUser(keycloakUserId);
        } catch (Exception ex) {
            throw new CompensationFailedException(
                    "Orphaned Keycloak user: " + keycloakUserId, ex);
        }
    }
}
