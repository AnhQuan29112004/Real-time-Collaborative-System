package com.collabnote.common.security;

import com.collabnote.auth.service.AuthSessionService;
import com.collabnote.common.exception.BusinessException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final Set<String> PUBLIC_POST_PATHS = Set.of("/api/v1/auth/register", "/api/v1/auth/login",
            "/api/v1/auth/refresh", "/api/v1/auth/logout", "/api/v1/auth/email/request-verification",
            "/api/v1/auth/email/verify", "/api/v1/auth/password/forgot", "/api/v1/auth/password/reset",
            "/api/v1/auth/google");
    private final JwtDecoder decoder;
    private final AuthSessionService sessions;
    private final JsonSecurityErrorHandler errors;
    public JwtAuthenticationFilter(JwtDecoder decoder, AuthSessionService sessions, JsonSecurityErrorHandler errors) {
        this.decoder = decoder; this.sessions = sessions; this.errors = errors;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && PUBLIC_POST_PATHS.contains(request.getServletPath());
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.startsWith("Bearer ") || header.length() > 8192) throw new BadJwtException("Invalid bearer header");
                var jwt = decoder.decode(header.substring(7));
                Long userId = Long.valueOf(jwt.getSubject());
                String sid = jwt.getClaimAsString("sid");
                if (sid == null) throw new BadJwtException("Missing session claim");
                UUID sessionId = UUID.fromString(sid);
                var user = sessions.authenticate(sessionId, userId);
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new AuthSessionAuthentication(new CollabPrincipal(user.id(), user.email()), sessionId));
                SecurityContextHolder.setContext(context);
            } catch (JwtException | IllegalArgumentException | BusinessException ex) {
                SecurityContextHolder.clearContext();
                errors.commence(request, response, new BadCredentialsException("Invalid access token"));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
