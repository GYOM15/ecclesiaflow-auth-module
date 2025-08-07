package com.ecclesiaflow.springsecurity.business.domain;

public class SigninCredentials {
    private final String email;
    private final String password;

    public SigninCredentials(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
