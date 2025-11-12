package com.ecclesiaflow.springsecurity.web.client;

import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import com.ecclesiaflow.springsecurity.io.members.MembersGrpcClient;
import com.ecclesiaflow.springsecurity.web.model.MemberConfirmationStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Implémentation REST (WebClient) du members Members.
 * <p>
 * <strong>⚠️ LEGACY / FALLBACK ONLY</strong>
 * </p>
 * <p>
 * Cette implémentation est conservée pour :
 * <ul>
 *   <li>Fallback si gRPC a un problème en production</li>
 *   <li>Migration progressive (canary deployment)</li>
 *   <li>Tests de régression (comparaison REST vs gRPC)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Production :</strong> Utiliser gRPC ({@link MembersGrpcClient}) avec grpc.enabled=true
 * </p>
 * <p>
 * <strong>TODO (2025-06) :</strong> Supprimer après Un délai de stabilité gRPC en production
 * </p>
 *
 * @deprecated Préférer {@link MembersGrpcClient} pour la communication inter-modules
 * @see MembersGrpcClient
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "grpc.enabled", havingValue = "false", matchIfMissing = true)
public class MembersClientImpl implements MembersClient {

    private final WebClient membersWebClient;

    @Override
    public boolean isEmailNotConfirmed(String email) {
        try {
            MemberConfirmationStatusResponse response = membersWebClient.get()
                    .uri("/ecclesiaflow/members/{email}/confirmation-status", email)
                    .retrieve()
                    .bodyToMono(MemberConfirmationStatusResponse.class)
                    .block(); // Blocage volontaire pour Spring MVC

            return response == null || response.getConfirmed() == null || !response.getConfirmed();
        } catch (WebClientResponseException.NotFound ex) {
            return true;
        }
    }
}
