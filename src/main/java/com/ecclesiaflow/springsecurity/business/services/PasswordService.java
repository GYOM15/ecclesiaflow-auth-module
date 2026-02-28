package com.ecclesiaflow.springsecurity.business.services;

public interface PasswordService {

    /**
     * Sets up initial password for a new user after email confirmation.
     *
     * @param setupToken Opaque setup token from email
     * @param password New password
     * @throws com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException if token is invalid or email not confirmed
     */
    void setupPassword(String setupToken, String password);
}
