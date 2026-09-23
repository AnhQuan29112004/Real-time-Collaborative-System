package com.collabnote.common.security;

import com.collabnote.auth.service.AuthSessionService;
import com.collabnote.common.config.*;
import com.collabnote.common.exception.ErrorResponseFactory;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonSecurityErrorHandler errors,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors, CsrfTokenRepository csrf,
            JwtDecoder decoder, AuthSessionService sessions, Clock clock,
            ErrorResponseFactory errorResponses, ObjectMapper mapper) throws Exception {
        var jwtFilter = new JwtAuthenticationFilter(decoder, sessions, errors);
        return http.cors(config -> config.configurationSource(cors))
                .csrf(config -> config.csrfTokenRepository(csrf))
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable).requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(config -> config.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new AuthRateLimitFilter(clock, errorResponses, mapper), JwtAuthenticationFilter.class)
                .authorizeHttpRequests(config -> config
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/liveness",
                                "/actuator/health/readiness", "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, JwtAuthenticationFilter.PUBLIC_POST_PATHS.toArray(String[]::new)).permitAll()
                        .requestMatchers("/api/v1/users/me", "/api/v1/auth/sessions", "/api/v1/auth/sessions/**",
                                "/api/v1/auth/password", "/api/v1/auth/google/link").authenticated()
                        .anyRequest().denyAll()).build();
    }
    @Bean
    CsrfTokenRepository csrfTokenRepository(AuthProperties properties) {
        var repository = new CookieCsrfTokenRepository();
        repository.setCookieName("COLLABNOTE_CSRF");
        repository.setHeaderName("X-CSRF-TOKEN");
        repository.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(properties.cookieSecure()).sameSite("Lax").path("/"));
        return repository;
    }
    @Bean
    CorsConfigurationSource corsConfigurationSource(WebProperties properties) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN"));
        config.setExposedHeaders(List.of("X-Request-Id"));
        config.setAllowCredentials(true); config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", config); return source;
    }
    @Bean
    PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }
}
