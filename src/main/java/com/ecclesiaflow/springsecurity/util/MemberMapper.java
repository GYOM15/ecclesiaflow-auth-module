package com.ecclesiaflow.springsecurity.util;

import com.ecclesiaflow.springsecurity.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.dto.SignUpRequest;
import com.ecclesiaflow.springsecurity.dto.SigninRequest;

public class MemberMapper {
    public static MemberRegistration fromSignUpRequest(SignUpRequest req) {
        return new MemberRegistration(
            req.getFirstName(),
            req.getLastName(),
            req.getEmail(),
            req.getPassword()
        );
    }

    public static SigninCredentials fromSigninRequest(SigninRequest req) {
        return new SigninCredentials(
            req.getEmail(),
            req.getPassword()
        );
    }
}
