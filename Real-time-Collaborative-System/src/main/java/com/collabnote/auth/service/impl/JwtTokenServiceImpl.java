package com.collabnote.auth.service.impl;

import com.collabnote.auth.dto.AuthResponse;
import com.collabnote.auth.service.JwtTokenService;
import com.collabnote.common.config.AuthProperties;
import com.collabnote.user.dto.UserResponse;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {
    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final Clock clock;
    public AuthResponse issue(UserResponse user, UUID sessionId, Instant sessionExpiresAt) {
        Instant now = Instant.now(clock);
        Instant expiry = now.plus(properties.accessTtl());
        if (expiry.isAfter(sessionExpiresAt)) expiry = sessionExpiresAt;
        var claims = JwtClaimsSet.builder().issuer(properties.issuer()).audience(List.of(properties.audience()))
                .subject(user.id().toString()).issuedAt(now).expiresAt(expiry)
                .id(UUID.randomUUID().toString()).claim("sid", sessionId.toString()).build();
        String access = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        return new AuthResponse(access, "Bearer", Duration.between(now, expiry).toSeconds(), user);
    }
}
