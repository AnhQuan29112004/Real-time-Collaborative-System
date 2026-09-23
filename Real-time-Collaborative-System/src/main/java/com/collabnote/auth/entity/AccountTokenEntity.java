package com.collabnote.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "auth_account_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountTokenEntity {

    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 16) private String purpose;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;

    public AccountTokenEntity(Long userId, String purpose, String hash, Instant now, Instant expiry) {
        id = UUID.randomUUID(); this.userId = userId; this.purpose = purpose;
        tokenHash = hash; createdAt = now; expiresAt = expiry;
    }
    public void consume(Instant now) { consumedAt = now; }

}
