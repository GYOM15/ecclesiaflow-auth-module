package com.ecclesiaflow.springsecurity.controller;

import com.ecclesiaflow.springsecurity.dto.UserResponse;
import com.ecclesiaflow.springsecurity.entities.User;
import com.ecclesiaflow.springsecurity.repository.UserRepository;
import com.ecclesiaflow.springsecurity.services.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final JWTService jwtService;
    private final UserRepository userRepository;

    @GetMapping("/hello")
    public ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hi User");
    }

//    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<UserResponse> getUserInfo() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();
//
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new IllegalStateException("User not found"));
//
//        UserResponse response = UserResponse.builder()
//                .message("Hi " + user.getFirstName())
//                .email(user.getEmail())
//                .build();
//
//        return ResponseEntity.ok(response);
//    }


    @GetMapping("/token")
    public ResponseEntity<UserResponse> getUserInfo(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build(); // Token manquant
        }

        String token = authorizationHeader.substring(7);
        System.out.println("Token reçu dans la requête : " + token);

        // Décoder le token pour vérifier les infos
        String email = jwtService.extractUserName(token);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));

        UserResponse response = UserResponse.builder().message("Hi " + user.getFirstName()).email(user.getEmail()).token(token).build();

        return ResponseEntity.ok(response);
    }

}
