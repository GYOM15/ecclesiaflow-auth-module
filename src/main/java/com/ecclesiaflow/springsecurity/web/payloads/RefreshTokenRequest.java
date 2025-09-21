package com.ecclesiaflow.springsecurity.web.payloads;

import lombok.Data;

@Data
public class RefreshTokenRequest {

    private String refreshToken;
}
