package com.ecclesiaflow.springsecurity.business.services;

import org.springframework.security.core.userdetails.UserDetailsService;

public interface MemberService {

    UserDetailsService userDetailsService();
}
