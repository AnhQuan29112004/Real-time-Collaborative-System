package com.collabnote.common.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {
    @Bean
    SecretKeySpec jwtSigningKey(AuthProperties properties) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(properties.jwtSecret()); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("AUTH_JWT_SECRET must be Base64 encoded"); }
        if (bytes.length < 32) throw new IllegalArgumentException("AUTH_JWT_SECRET requires at least 32 random bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean
    JwtEncoder jwtEncoder(SecretKeySpec jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }
    @Bean
    JwtDecoder jwtDecoder(SecretKeySpec jwtSigningKey, AuthProperties properties) {
        var decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> audience = token -> token.getAudience().contains(properties.audience())
                && token.getExpiresAt() != null && token.getSubject() != null && token.hasClaim("sid")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(properties.issuer()), audience));
        return decoder;
    }
}
