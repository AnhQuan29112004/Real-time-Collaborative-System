package com.collabnote.user.entity;

import com.collabnote.common.audit.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;
    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;
    @Column(name = "disabled_at")
    private Instant disabledAt;
    @Version
    private long version;

    public UserEntity(String email, String displayName, Instant verifiedAt) {
        this.email = email;
        this.displayName = displayName;
        this.emailVerifiedAt = verifiedAt;
    }
    public void updateProfile(String displayName) { this.displayName = displayName; }
    public void verifyEmail(Instant now) { this.emailVerifiedAt = now; }
}
