package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.services.MembersModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MembersModuleServiceImpl implements MembersModuleService {

    private final WebClient membersWebClient;

    @Override
    public boolean isEmailNotConfirmed(String email) {
        try {
            Map response = membersWebClient.get()
                    .uri("/ecclesiaflow/members/{email}/confirmation-status", email)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // Blocage volontaire pour Spring MVC

            return response == null || !Boolean.TRUE.equals(response.get("confirmed"));
        } catch (WebClientResponseException.NotFound ex) {
            return true;
        }
    }
}
