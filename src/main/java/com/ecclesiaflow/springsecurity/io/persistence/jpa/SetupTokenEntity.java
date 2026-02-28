package com.ecclesiaflow.springsecurity.io.persistence.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for setup_token table.
 * Infrastructure layer - contains all JPA annotations.
 */
@Entity
@Table(name = "setup_token", indexes = {
    @Index(name = "idx_token_hash", columnList = "token_hash"),
    @Index(name = "idx_member_id", columnList = "member_id"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupTokenEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @UuidGenerator
    @Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private String email;

    @Column(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SetupToken.TokenPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SetupToken.TokenStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
