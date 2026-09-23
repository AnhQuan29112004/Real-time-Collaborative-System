package com.collabnote.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "auth_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthSessionEntity {

    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;

    public AuthSessionEntity(Long userId, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID(); this.userId = userId; this.createdAt = now; this.expiresAt = expiresAt;
    }
    public void revoke(Instant now) { this.revokedAt = now; }
    public boolean isActive(Instant now) { return revokedAt == null && expiresAt.isAfter(now); }

}
