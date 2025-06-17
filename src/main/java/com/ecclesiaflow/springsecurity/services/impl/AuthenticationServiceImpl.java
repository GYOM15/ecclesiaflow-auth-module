package com.ecclesiaflow.springsecurity.services.impl;


import com.ecclesiaflow.springsecurity.dto.SignUpRequest;

import com.ecclesiaflow.springsecurity.entities.Role;
import com.ecclesiaflow.springsecurity.entities.User;
import com.ecclesiaflow.springsecurity.repository.UserRepository;
import com.ecclesiaflow.springsecurity.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.services.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public User signup(SignUpRequest signUpRequest) {
        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà.");
        }
        User user = new User();

        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setRole(Role.USER);

        return userRepository.save(user);
    }


}
