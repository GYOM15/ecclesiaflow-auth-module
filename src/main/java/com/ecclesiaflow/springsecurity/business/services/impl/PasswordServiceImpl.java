package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.business.events.PasswordSetEvent;
import com.ecclesiaflow.springsecurity.business.exceptions.CompensationFailedException;
import com.ecclesiaflow.springsecurity.business.exceptions.InvalidTokenException;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.business.services.SetupTokenService;
import com.ecclesiaflow.springsecurity.io.keycloak.KeycloakAdminClient;
import com.ecclesiaflow.springsecurity.web.constants.Messages;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final SetupTokenService setupTokenService;
    private final KeycloakAdminClient keycloakAdminClient;
    private final MembersClient membersClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void setupPassword(String setupToken, String password) {
        SetupToken token = null;
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

            eventPublisher.publishEvent(new PasswordSetEvent(this, token.getEmail()));

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
