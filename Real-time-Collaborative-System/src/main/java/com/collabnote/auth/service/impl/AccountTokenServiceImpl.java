package com.collabnote.auth.service.impl;

import com.collabnote.auth.dto.AccountTokenDelivery;
import com.collabnote.auth.entity.AccountTokenEntity;
import com.collabnote.auth.repository.AccountTokenRepository;
import com.collabnote.auth.service.*;
import com.collabnote.auth.validator.AuthInput;
import com.collabnote.common.config.AuthProperties;
import com.collabnote.common.exception.*;
import com.collabnote.common.security.SecureTokens;
import com.collabnote.user.service.UserService;
import java.time.*;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountTokenServiceImpl implements AccountTokenService {
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    private final AccountTokenRepository tokens;
    private final CredentialService credentials;
    private final UserService users;
    private final AuthProperties properties;
    private final Clock clock;

    @Transactional
    public Optional<AccountTokenDelivery> issue(String email, String purpose) {
        if (!purpose.equals("VERIFY_EMAIL") && !purpose.equals("RESET_PASSWORD"))
            throw new IllegalArgumentException("Unsupported account token purpose");
        var candidate = users.findByEmail(AuthInput.email(email));
        if (candidate.isEmpty() || candidate.get().disabledAt() != null) return Optional.empty();
        var user = users.lockActiveUser(candidate.get().id());
        if (purpose.equals("VERIFY_EMAIL") && user.emailVerifiedAt() != null) return Optional.empty();
        if (purpose.equals("RESET_PASSWORD") && (user.emailVerifiedAt() == null || !credentials.hasLocalIdentity(user.id())))
            return Optional.empty();
        Instant now = Instant.now(clock);
        if (tokens.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.id(), purpose)
                .filter(t -> t.getCreatedAt().plus(RESEND_COOLDOWN).isAfter(now)).isPresent()) return Optional.empty();
        tokens.invalidate(user.id(), purpose, now);
        String raw = SecureTokens.generate();
        Duration ttl = purpose.equals("VERIFY_EMAIL") ? properties.verificationTtl() : properties.resetTtl();
        tokens.save(new AccountTokenEntity(user.id(), purpose, SecureTokens.hash(raw), now, now.plus(ttl)));
        return Optional.of(new AccountTokenDelivery(user.email(), raw, purpose));
    }

    @Transactional
    public void verifyEmail(String raw) {
        var token = consume(raw, "VERIFY_EMAIL");
        users.verifyEmail(token.getUserId());
        tokens.invalidate(token.getUserId(), "VERIFY_EMAIL", Instant.now(clock));
    }

    @Transactional
    public void resetPassword(String raw, String newPassword) {
        AuthInput.password(newPassword);
        var token = consume(raw, "RESET_PASSWORD");
        credentials.resetPassword(token.getUserId(), newPassword);
    }

    private AccountTokenEntity consume(String raw, String purpose) {
        if (!SecureTokens.isValidFormat(raw)) throw invalid();
        String hash = SecureTokens.hash(raw);
        Long owner = tokens.findOwner(hash, purpose).orElseThrow(this::invalid);
        users.lockActiveUser(owner);
        var token = tokens.findByTokenHashAndPurpose(hash, purpose).orElseThrow(this::invalid);
        Instant now = Instant.now(clock);
        if (token.getConsumedAt() != null || !token.getExpiresAt().isAfter(now)) throw invalid();
        token.consume(now);
        return token;
    }
    private BusinessException invalid() { return new BusinessException(ErrorCode.INVALID_ACCOUNT_TOKEN); }
}
