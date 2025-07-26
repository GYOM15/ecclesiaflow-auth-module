package com.ecclesiaflow.springsecurity.controller;

import com.ecclesiaflow.springsecurity.dto.MemberResponse;
import com.ecclesiaflow.springsecurity.dto.SignUpRequest;
import com.ecclesiaflow.springsecurity.entities.Member;
import com.ecclesiaflow.springsecurity.repository.MemberRepository;
import com.ecclesiaflow.springsecurity.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.services.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MembersController {
    private final JWTService jwtService;
    private final MemberRepository memberRepository;
    private final AuthenticationService authenticationService;

    @GetMapping("/hello")
    public ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hi Member");
    }

    @PostMapping(value = "/signup", produces = "application/vnd.ecclesiaflow.members.v2+json")
    public ResponseEntity<Member> signup(@RequestBody SignUpRequest signUpRequest) {
        return ResponseEntity.ok(authenticationService.signup(signUpRequest));
    }

//    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<MemberResponse> getUserInfo() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();
//
//        Member user = memberRepository.findByEmail(email)
//                .orElseThrow(() -> new IllegalStateException("Member not found"));
//
//        MemberResponse response = MemberResponse.builder()
//                .message("Hi " + user.getFirstName())
//                .email(user.getEmail())
//                .build();
//
//        return ResponseEntity.ok(response);
//    }


    @GetMapping("/token")
    public ResponseEntity<MemberResponse> getUserInfo(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build(); // Token manquant
        }

        String token = authorizationHeader.substring(7);
        System.out.println("Token reçu dans la requête : " + token);

        // Décoder le token pour vérifier les infos
        String email = jwtService.extractUserName(token);
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("Member not found"));

        MemberResponse response = MemberResponse.builder().message("Hi " + member.getFirstName()).email(member.getEmail()).token(token).build();

        return ResponseEntity.ok(response);
    }

}
