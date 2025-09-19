package com.ecclesiaflow.springsecurity.web.mappers;


import com.ecclesiaflow.springsecurity.business.domain.PasswordManagement;
import com.ecclesiaflow.springsecurity.web.dto.PasswordManagementResponse;
import com.ecclesiaflow.springsecurity.web.dto.SetPasswordRequest;
import org.springframework.stereotype.Component;

@Component
public class PasswordManagementMapper {

    public PasswordManagement fromSetPasswordRequest(SetPasswordRequest passwordRequest) {
        return new PasswordManagement(passwordRequest.getEmail(),
                                      passwordRequest.getPassword(),
                                      passwordRequest.getTemporaryToken());
    }

    public static PasswordManagementResponse toDto(String message) {
        return PasswordManagementResponse.builder()
                .message(message)
                .build();
    }
}
