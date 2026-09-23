package com.collabnote.common.security;

import com.collabnote.auth.dto.IssuedTokens;
import com.collabnote.common.config.AuthProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookies {
    public static final String REFRESH_COOKIE = "COLLABNOTE_REFRESH";
    private final AuthProperties properties;
    private final Clock clock;
    private final CsrfTokenRepository csrf;
    public void issue(IssuedTokens tokens, HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(tokens.refreshToken(),
                Duration.between(Instant.now(clock), tokens.refreshExpiresAt())).toString());
        csrf.saveToken(null, request, response);
    }
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
        csrf.saveToken(null, request, response);
    }
    private ResponseCookie cookie(String value, Duration age) {
        return ResponseCookie.from(REFRESH_COOKIE, value).httpOnly(true).secure(properties.cookieSecure())
                .sameSite("Lax").path("/api/v1/auth").maxAge(age.isNegative() ? Duration.ZERO : age).build();
    }
}
