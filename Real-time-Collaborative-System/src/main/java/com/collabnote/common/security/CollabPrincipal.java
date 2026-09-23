package com.collabnote.common.security;

import java.security.Principal;
import java.util.Objects;

/** Created only by the authentication adapter after identity verification. */
public record CollabPrincipal(Long userId, String email) implements Principal {
    public CollabPrincipal {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(email, "email");
        if (userId <= 0 || email.isBlank()) {
            throw new IllegalArgumentException("Principal requires a positive user ID and email");
        }
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
