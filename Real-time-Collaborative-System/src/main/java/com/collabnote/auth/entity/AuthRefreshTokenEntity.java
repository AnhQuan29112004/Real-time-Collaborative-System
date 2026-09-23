package com.collabnote.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "auth_refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthRefreshTokenEntity {

    @Id private UUID id;
    @Column(name = "session_id", nullable = false) private UUID sessionId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;

    public AuthRefreshTokenEntity(UUID sessionId, String hash, Instant expiry) {
        this.id = UUID.randomUUID(); this.sessionId = sessionId; this.tokenHash = hash; this.expiresAt = expiry;
    }
    public void consume(Instant now) { consumedAt = now; }

}
