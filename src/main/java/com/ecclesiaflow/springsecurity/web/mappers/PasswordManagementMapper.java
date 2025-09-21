package com.ecclesiaflow.springsecurity.web.mappers;

import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.web.dto.PasswordManagementResponse;
import org.springframework.stereotype.Component;

@Component
public class PasswordManagementMapper {

    public static PasswordManagementResponse toDtoWithTokens(String message, UserTokens userTokens, Long expiresIn) {
        return PasswordManagementResponse.builder()
                .message(message)
                .accessToken(userTokens.accessToken())
                .refreshToken(userTokens.refreshToken())
                .expiresIn(expiresIn)
                .build();
    }
}
