package com.ecclesiaflow.springsecurity.business.mappers;

import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.business.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.web.dto.SignUpRequest;
import com.ecclesiaflow.springsecurity.web.dto.SigninRequest;

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
