package com.collabnote.auth.dto;
import java.time.Instant;
/** Internal transport result; refresh token must only be written to an HttpOnly cookie. */
public record IssuedTokens(AuthResponse response, String refreshToken, Instant refreshExpiresAt) {
    @Override public String toString() { return "IssuedTokens[redacted]"; }
}
