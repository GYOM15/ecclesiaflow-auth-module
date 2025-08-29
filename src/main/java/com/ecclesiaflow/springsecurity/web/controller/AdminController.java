package com.ecclesiaflow.springsecurity.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ecclesiaflow/adminMembers")
@RequiredArgsConstructor
public class AdminController {

    @GetMapping(produces = "application/vnd.ecclesiaflow.admins.v2+json")
    public ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hi Admin");
    }
}
