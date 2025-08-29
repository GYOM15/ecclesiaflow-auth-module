package com.ecclesiaflow.springsecurity.web.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Data
@Builder
public class MemberResponse {
    private String message;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private String role;
    private boolean accountNonLocked;
    private boolean enabled;
    private String token;
    private Collection<? extends GrantedAuthority> authorities;
    private String username;
    private boolean accountNonExpired;
    private boolean credentialsNonExpired;
}

