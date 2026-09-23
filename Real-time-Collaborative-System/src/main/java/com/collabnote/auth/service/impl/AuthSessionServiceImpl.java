package com.collabnote.auth.service.impl;

import com.collabnote.auth.dto.*;
import com.collabnote.auth.dto.response.SessionResponse;
import com.collabnote.auth.entity.*;
import com.collabnote.auth.mapper.AuthMapper;
import com.collabnote.auth.repository.*;
import com.collabnote.auth.service.*;
import com.collabnote.common.config.AuthProperties;
import com.collabnote.common.exception.*;
import com.collabnote.common.security.SecureTokens;
import com.collabnote.user.dto.UserResponse;
import com.collabnote.user.service.UserService;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class AuthSessionServiceImpl implements AuthSessionService {
    private final AuthSessionRepository sessions;
    private final AuthRefreshTokenRepository refreshTokens;
    private final UserService users;
    private final JwtTokenService jwt;
    private final AuthMapper mapper;
    private final AuthProperties properties;
    private final Clock clock;

    @Transactional
    public IssuedTokens issue(UserResponse user) {
        user = users.lockActiveUser(user.id());
        if (user.emailVerifiedAt() == null) throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        Instant now = Instant.now(clock);
        var active = sessions.findActive(user.id(), now);
        if (active.size() >= properties.maxSessions())
            active.subList(properties.maxSessions() - 1, active.size()).forEach(s -> s.revoke(now));
        var session = sessions.save(new AuthSessionEntity(user.id(), now, now.plus(properties.sessionTtl())));
        return tokens(user, session);
    }

    // A replay must commit revocation even though the response is an authentication error.
    @Transactional(noRollbackFor = BusinessException.class)
    public IssuedTokens refresh(String raw) {
        if (!SecureTokens.isValidFormat(raw)) throw invalidRefresh();
        String hash = SecureTokens.hash(raw);
        Long owner = refreshTokens.findOwnerByHash(hash).orElseThrow(this::invalidRefresh);
        var user = users.lockActiveUser(owner);
        var token = refreshTokens.findByTokenHash(hash).orElseThrow(this::invalidRefresh);
        var session = sessions.findById(token.getSessionId()).orElseThrow(this::invalidRefresh);
        Instant now = Instant.now(clock);
        if (!session.isActive(now) || !token.getExpiresAt().isAfter(now)) throw invalidRefresh();
        if (token.getConsumedAt() != null) {
            session.revoke(now);
            throw invalidRefresh();
        }
        if (user.emailVerifiedAt() == null) throw invalidRefresh();
        token.consume(now);
        return tokens(user, session);
    }

    @Transactional(readOnly = true)
    public UserResponse authenticate(UUID sessionId, Long userId) {
        var session = sessions.findById(sessionId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!session.getUserId().equals(userId) || !session.isActive(Instant.now(clock)))
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        var user = users.getActiveUser(userId);
        if (user.emailVerifiedAt() == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return user;
    }

    @Transactional
    public void logout(String raw) {
        if (!SecureTokens.isValidFormat(raw)) return;
        String hash = SecureTokens.hash(raw);
        refreshTokens.findOwnerByHash(hash).ifPresent(userId -> {
            users.lockActiveUser(userId);
            refreshTokens.findByTokenHash(hash).flatMap(t -> sessions.findById(t.getSessionId()))
                    .ifPresent(s -> s.revoke(Instant.now(clock)));
        });
    }

    @Transactional
    public void revoke(Long userId, UUID sessionId) {
        users.lockActiveUser(userId);
        var session = sessions.findById(sessionId).filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        session.revoke(Instant.now(clock));
    }

    @Transactional
    public void revokeAll(Long userId) {
        users.lockActiveUser(userId);
        sessions.revokeAll(userId, Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> list(Long userId, UUID current) {
        return sessions.findActive(userId, Instant.now(clock)).stream()
                .map(s -> mapper.toResponse(s, s.getId().equals(current))).toList();
    }

    private IssuedTokens tokens(UserResponse user, AuthSessionEntity session) {
        String raw = SecureTokens.generate();
        refreshTokens.save(new AuthRefreshTokenEntity(session.getId(), SecureTokens.hash(raw), session.getExpiresAt()));
        return new IssuedTokens(jwt.issue(user, session.getId(), session.getExpiresAt()), raw, session.getExpiresAt());
    }
    private BusinessException invalidRefresh() { return new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN); }
}
