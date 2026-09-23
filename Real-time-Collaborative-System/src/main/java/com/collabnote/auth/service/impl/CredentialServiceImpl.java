package com.collabnote.auth.service.impl;

import com.collabnote.auth.dto.*;
import com.collabnote.auth.dto.request.*;
import com.collabnote.auth.entity.AuthIdentityEntity;
import com.collabnote.auth.repository.*;
import com.collabnote.auth.service.*;
import com.collabnote.auth.validator.AuthInput;
import com.collabnote.common.exception.*;
import com.collabnote.user.dto.UserResponse;
import com.collabnote.user.service.UserService;
import java.time.*;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class CredentialServiceImpl implements CredentialService {
    private final UserService users;
    private final AuthIdentityRepository identities;
    private final AuthSessionService sessions;
    private final AccountTokenRepository accountTokens;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final String dummyHash;

    public CredentialServiceImpl(UserService users, AuthIdentityRepository identities, AuthSessionService sessions,
            AccountTokenRepository accountTokens, PasswordEncoder passwords, Clock clock) {
        this.users = users; this.identities = identities; this.sessions = sessions;
        this.accountTokens = accountTokens; this.passwords = passwords; this.clock = clock;
        dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        AuthInput.password(request.password());
        String email = AuthInput.email(request.email());
        if (users.findByEmail(email).isPresent()) throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        var user = users.createUser(email, request.displayName().strip(), false);
        identities.saveAndFlush(new AuthIdentityEntity(user.id(), "LOCAL", email, passwords.encode(request.password())));
        return user;
    }

    @Transactional
    public IssuedTokens login(LoginRequest request) {
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        var owner = identities.findOwner("LOCAL", AuthInput.email(request.email())).orElse(null);
        if (owner == null) {
            passwords.matches(request.password(), dummyHash);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        var user = users.lockActiveUser(owner);
        // Load credentials after the user lock to serialize login with password reset.
        var identity = identities.findByUserIdAndProvider(user.id(), "LOCAL")
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwords.matches(request.password(), identity.getPasswordHash()))
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        if (user.emailVerifiedAt() == null) throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        return sessions.issue(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        AuthInput.password(request.newPassword());
        users.lockActiveUser(userId);
        var identity = identities.findByUserIdAndProvider(userId, "LOCAL")
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCAL_PASSWORD_UNAVAILABLE));
        if (!passwords.matches(request.currentPassword(), identity.getPasswordHash()))
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        identity.changePassword(passwords.encode(request.newPassword()));
        sessions.revokeAll(userId);
        accountTokens.invalidate(userId, "RESET_PASSWORD", Instant.now(clock));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void resetPassword(Long userId, String password) {
        AuthInput.password(password);
        users.lockActiveUser(userId);
        var identity = identities.findByUserIdAndProvider(userId, "LOCAL")
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ACCOUNT_TOKEN));
        identity.changePassword(passwords.encode(password));
        sessions.revokeAll(userId);
        accountTokens.invalidate(userId, "RESET_PASSWORD", Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public boolean hasLocalIdentity(Long userId) { return identities.findByUserIdAndProvider(userId, "LOCAL").isPresent(); }
}
