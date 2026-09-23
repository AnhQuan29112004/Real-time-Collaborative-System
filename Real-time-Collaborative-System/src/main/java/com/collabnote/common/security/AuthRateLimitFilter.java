package com.collabnote.common.security;

import com.collabnote.common.exception.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/** Single-instance guard. Shared rate limiting is required before scaling auth across nodes. */
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final int REQUESTS_PER_MINUTE = 30;
    private static final int MAX_CLIENTS = 10000;
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;
    private final ErrorResponseFactory errors;
    private final ObjectMapper mapper;
    private Instant nextCleanup = Instant.MIN;
    public AuthRateLimitFilter(Clock clock, ErrorResponseFactory errors, ObjectMapper mapper) {
        this.clock = clock; this.errors = errors; this.mapper = mapper;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/v1/auth/");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!allow(request.getRemoteAddr())) {
            response.setStatus(429); response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8"); response.setHeader("Retry-After", "60");
            mapper.writeValue(response.getOutputStream(), errors.create(ErrorCode.TOO_MANY_REQUESTS, request.getLocale()));
            return;
        }
        chain.doFilter(request, response);
    }
    private synchronized boolean allow(String client) {
        Instant now = Instant.now(clock);
        if (!now.isBefore(nextCleanup)) {
            windows.entrySet().removeIf(entry -> !entry.getValue().until().isAfter(now));
            nextCleanup = now.plusSeconds(60);
        }
        Window window = windows.get(client);
        if (window == null || !window.until().isAfter(now)) {
            if (window == null && windows.size() >= MAX_CLIENTS) return false;
            windows.put(client, new Window(now.plusSeconds(60), 1)); return true;
        }
        if (window.count() >= REQUESTS_PER_MINUTE) return false;
        windows.put(client, new Window(window.until(), window.count() + 1)); return true;
    }
    private record Window(Instant until, int count) { }
}
