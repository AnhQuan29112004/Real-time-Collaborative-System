package com.collabnote.auth.controller;

import com.collabnote.auth.dto.response.SessionResponse;
import com.collabnote.auth.service.AuthSessionService;
import com.collabnote.common.response.ApiResponse;
import com.collabnote.common.security.*;
import jakarta.servlet.http.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/sessions")
@RequiredArgsConstructor
public class AuthSessionController {
    private final AuthSessionService sessions;
    private final AuthCookies cookies;
    @GetMapping
    public ApiResponse<List<SessionResponse>> list(AuthSessionAuthentication authentication) {
        return ApiResponse.success(sessions.list(authentication.getPrincipal().userId(), authentication.getSessionId()));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> revoke(@PathVariable UUID id, AuthSessionAuthentication authentication) {
        sessions.revoke(authentication.getPrincipal().userId(), id); return ApiResponse.success();
    }
    @DeleteMapping
    public ApiResponse<Void> revokeAll(AuthSessionAuthentication authentication, HttpServletRequest request, HttpServletResponse response) {
        sessions.revokeAll(authentication.getPrincipal().userId()); cookies.clear(request, response); return ApiResponse.success();
    }
}
