package com.ecclesiaflow.springsecurity.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PasswordManagementResponse {
    private String message;
}
