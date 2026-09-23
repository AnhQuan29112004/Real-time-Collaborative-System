package com.collabnote.auth.service.impl;

import com.collabnote.auth.dto.GoogleIdentity;
import com.collabnote.auth.service.GoogleTokenService;
import com.collabnote.auth.validator.AuthInput;
import com.collabnote.common.config.AuthProperties;
import com.collabnote.common.exception.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
public class GoogleTokenServiceImpl implements GoogleTokenService {
    private final AuthProperties properties;
    private final NimbusJwtDecoder decoder;
    public GoogleTokenServiceImpl(AuthProperties properties) {
        this.properties = properties;
        var http = new SimpleClientHttpRequestFactory();
        http.setConnectTimeout(5000); http.setReadTimeout(5000);
        decoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .restOperations(new RestTemplate(http)).build();
        OAuth2TokenValidator<Jwt> claims = jwt -> jwt.getAudience().contains(properties.googleClientId())
                && jwt.getExpiresAt() != null && jwt.getSubject() != null
                && Boolean.TRUE.equals(jwt.getClaim("email_verified"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("https://accounts.google.com"), claims));
    }
    public GoogleIdentity verify(String rawToken) {
        if (properties.googleClientId().isBlank()) throw new BusinessException(ErrorCode.GOOGLE_LOGIN_UNAVAILABLE);
        try {
            var jwt = decoder.decode(rawToken);
            if (jwt.getSubject() == null || jwt.getSubject().isBlank() || jwt.getSubject().length() > 254)
                throw new BadJwtException("Invalid subject claim");
            String email = jwt.getClaimAsString("email");
            if (email == null || email.length() > 254 || !email.contains("@")) throw new BadJwtException("Invalid email claim");
            String name = jwt.getClaimAsString("name");
            if (name == null || name.isBlank()) name = "CollabNote user";
            name = name.strip();
            return new GoogleIdentity(jwt.getSubject(), AuthInput.email(email), name.substring(0, Math.min(name.length(), 100)));
        } catch (JwtException | IllegalArgumentException ex) { throw new BusinessException(ErrorCode.INVALID_CREDENTIALS); }
    }
}
