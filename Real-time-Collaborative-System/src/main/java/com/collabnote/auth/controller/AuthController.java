package com.collabnote.auth.controller;

import com.collabnote.auth.dto.*;
import com.collabnote.auth.dto.request.*;
import com.collabnote.auth.dto.response.CsrfResponse;
import com.collabnote.auth.service.*;
import com.collabnote.common.response.ApiResponse;
import com.collabnote.common.security.AuthCookies;
import com.collabnote.common.util.SecurityUtils;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final AuthSessionService sessions;
    private final GoogleIdentityService google;
    private final AuthCookies cookies;

    @GetMapping("/csrf")
    public ApiResponse<CsrfResponse> csrf(CsrfToken token) {
        return ApiResponse.success(new CsrfResponse(token.getHeaderName(), token.getToken()));
    }
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        auth.register(request); return ApiResponse.success();
    }
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        var tokens = auth.login(body); cookies.issue(tokens, request, response); return ApiResponse.success(tokens.response());
    }
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@CookieValue(name = AuthCookies.REFRESH_COOKIE, required = false) String refresh,
            HttpServletRequest request, HttpServletResponse response) {
        var tokens = sessions.refresh(refresh); cookies.issue(tokens, request, response); return ApiResponse.success(tokens.response());
    }
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CookieValue(name = AuthCookies.REFRESH_COOKIE, required = false) String refresh,
            HttpServletRequest request, HttpServletResponse response) {
        sessions.logout(refresh); cookies.clear(request, response); return ApiResponse.success();
    }
    @PostMapping("/email/request-verification")
    public ApiResponse<Void> requestVerification(@Valid @RequestBody EmailRequest request) {
        auth.requestVerification(request.email()); return ApiResponse.success();
    }
    @PostMapping("/email/verify")
    public ApiResponse<Void> verify(@Valid @RequestBody TokenRequest request) {
        auth.verifyEmail(request.token()); return ApiResponse.success();
    }
    @PostMapping("/password/forgot")
    public ApiResponse<Void> forgot(@Valid @RequestBody EmailRequest request) {
        auth.forgotPassword(request.email()); return ApiResponse.success();
    }
    @PostMapping("/password/reset")
    public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordRequest body, HttpServletRequest request, HttpServletResponse response) {
        auth.resetPassword(body); cookies.clear(request, response); return ApiResponse.success();
    }
    @PutMapping("/password")
    public ApiResponse<Void> password(@Valid @RequestBody ChangePasswordRequest body, HttpServletRequest request, HttpServletResponse response) {
        auth.changePassword(SecurityUtils.requireCurrentUserId(), body);
        cookies.clear(request, response); return ApiResponse.success();
    }
    @PostMapping("/google")
    public ApiResponse<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        var tokens = google.login(body.idToken()); cookies.issue(tokens, request, response); return ApiResponse.success(tokens.response());
    }
    @PostMapping("/google/link")
    public ApiResponse<Void> linkGoogle(@Valid @RequestBody GoogleLoginRequest body) {
        google.link(SecurityUtils.requireCurrentUserId(), body.idToken()); return ApiResponse.success();
    }
}
