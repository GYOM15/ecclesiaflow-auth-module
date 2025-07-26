package com.ecclesiaflow.springsecurity.util;

import com.ecclesiaflow.springsecurity.dto.MemberResponse;
import com.ecclesiaflow.springsecurity.entities.Member;

public class MemberResponseMapper {
    public static MemberResponse fromMember(Member member, String message, String token) {
        return MemberResponse.builder()
                .message(message)
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .password(member.getPassword())
                .email(member.getEmail())
                .role(member.getRole().name())
                .accountNonLocked(member.isAccountNonLocked())
                .enabled(member.isEnabled())
                .authorities(member.getAuthorities())
                .username(member.getUsername())
                .accountNonExpired(member.isAccountNonExpired())
                .credentialsNonExpired(member.isCredentialsNonExpired())
                .token(token)
                .build();
    }
    public static MemberResponse fromMember(Member member, String message) {
        return fromMember(member, message, null);
    }
}
