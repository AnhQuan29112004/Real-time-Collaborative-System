package com.collabnote.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "auth_identities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthIdentityEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 16) private String provider;
    @Column(nullable = false, length = 254) private String subject;
    @Column(name = "password_hash", length = 255) private String passwordHash;

    public AuthIdentityEntity(Long userId, String provider, String subject, String passwordHash) {
        this.userId = userId; this.provider = provider; this.subject = subject; this.passwordHash = passwordHash;
    }
    public void changePassword(String hash) { this.passwordHash = hash; }

}
