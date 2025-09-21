package com.ecclesiaflow.springsecurity.web.mappers;

import com.ecclesiaflow.springsecurity.business.domain.token.Tokens;
import com.ecclesiaflow.springsecurity.web.dto.PasswordManagementResponse;
import org.springframework.stereotype.Component;

@Component
public class PasswordManagementMapper {

    public static PasswordManagementResponse toDtoWithTokens(String message, Tokens tokens, Long expiresIn) {
        return PasswordManagementResponse.builder()
                .message(message)
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expiresIn(expiresIn)
                .build();
    }
}
