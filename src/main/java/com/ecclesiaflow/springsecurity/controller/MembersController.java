package com.ecclesiaflow.springsecurity.controller;

import com.ecclesiaflow.springsecurity.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.dto.MemberResponse;
import com.ecclesiaflow.springsecurity.dto.SignUpRequest;
import com.ecclesiaflow.springsecurity.entities.Member;
import com.ecclesiaflow.springsecurity.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.util.MemberMapper;
import com.ecclesiaflow.springsecurity.util.MemberResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MembersController {
    private final AuthenticationService authenticationService;

    @GetMapping(value = "/hello", produces = "application/vnd.ecclesiaflow.members.v2+json")
    public ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hi Member");
    }

    @PostMapping(value = "/signup", produces = "application/vnd.ecclesiaflow.members.v2+json")
    public ResponseEntity<MemberResponse> registerMember(@RequestBody SignUpRequest signUpRequest) {
        MemberRegistration registration = MemberMapper.fromSignUpRequest(signUpRequest);
        Member member = authenticationService.registerMember(registration);
        MemberResponse response = MemberResponseMapper.fromMember(member, "Member registered");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
