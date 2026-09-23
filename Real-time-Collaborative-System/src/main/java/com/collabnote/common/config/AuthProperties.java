package com.collabnote.common.config;

import jakarta.validation.constraints.*;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("collabnote.auth")
public record AuthProperties(
        @NotBlank String jwtSecret,
        @DefaultValue("collabnote") @NotBlank String issuer,
        @DefaultValue("collabnote-api") @NotBlank String audience,
        @DefaultValue("15m") Duration accessTtl,
        @DefaultValue("7d") Duration sessionTtl,
        @DefaultValue("24h") Duration verificationTtl,
        @DefaultValue("30m") Duration resetTtl,
        @DefaultValue("true") boolean cookieSecure,
        @DefaultValue("10") @Min(1) @Max(50) int maxSessions,
        @DefaultValue("false") boolean mailEnabled,
        @DefaultValue("noreply@localhost") @NotBlank String mailFrom,
        @DefaultValue("http://localhost:3000/verify-email") String verificationUrl,
        @DefaultValue("http://localhost:3000/reset-password") String resetUrl,
        @DefaultValue("") String googleClientId) {
    public AuthProperties {
        for (Duration ttl : new Duration[]{accessTtl, sessionTtl, verificationTtl, resetTtl}) {
            if (ttl == null || ttl.isNegative() || ttl.isZero())
                throw new IllegalArgumentException("Authentication TTLs must be positive");
        }
        if (accessTtl.compareTo(Duration.ofHours(1)) > 0 || accessTtl.compareTo(sessionTtl) > 0)
            throw new IllegalArgumentException("Access TTL must not exceed one hour or session TTL");
        for (String url : new String[]{verificationUrl, resetUrl}) {
            var uri = java.net.URI.create(url);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                    || uri.getRawFragment() != null || uri.getRawUserInfo() != null)
                throw new IllegalArgumentException("Account action URLs must be HTTP(S) URLs without fragments or credentials");
        }
    }
}
