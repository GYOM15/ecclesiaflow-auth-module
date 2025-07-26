package com.ecclesiaflow.springsecurity.repository;

import com.ecclesiaflow.springsecurity.entities.Member;
import com.ecclesiaflow.springsecurity.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByEmail(String email);

    Member findByRole (Role role);
}
