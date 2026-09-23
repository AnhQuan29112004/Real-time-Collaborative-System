package com.collabnote.auth.service.impl;

import com.collabnote.auth.dto.*;
import com.collabnote.auth.entity.AuthIdentityEntity;
import com.collabnote.auth.repository.AuthIdentityRepository;
import com.collabnote.auth.service.*;
import com.collabnote.common.exception.*;
import com.collabnote.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoogleAccountServiceImpl implements GoogleAccountService {
    private final AuthIdentityRepository identities;
    private final UserService users;
    private final AuthSessionService sessions;
    @Transactional
    public IssuedTokens login(GoogleIdentity google) {
        var identity = identities.findByProviderAndSubject("GOOGLE", google.subject());
        if (identity.isPresent()) return sessions.issue(users.getActiveUser(identity.get().getUserId()));
        if (users.findByEmail(google.email()).isPresent()) throw new BusinessException(ErrorCode.ACCOUNT_LINK_REQUIRED);
        var user = users.createUser(google.email(), google.displayName(), true);
        identities.saveAndFlush(new AuthIdentityEntity(user.id(), "GOOGLE", google.subject(), null));
        return sessions.issue(user);
    }
    @Transactional
    public void link(Long userId, GoogleIdentity google) {
        var user = users.lockActiveUser(userId);
        if (user.emailVerifiedAt() == null || !user.email().equals(google.email()))
            throw new BusinessException(ErrorCode.ACCOUNT_LINK_REQUIRED);
        var existing = identities.findByProviderAndSubject("GOOGLE", google.subject());
        if (existing.isPresent()) {
            if (!existing.get().getUserId().equals(userId)) throw new BusinessException(ErrorCode.ACCOUNT_LINK_REQUIRED);
            return;
        }
        if (identities.findByUserIdAndProvider(userId, "GOOGLE").isPresent())
            throw new BusinessException(ErrorCode.ACCOUNT_LINK_REQUIRED);
        identities.saveAndFlush(new AuthIdentityEntity(userId, "GOOGLE", google.subject(), null));
    }
}
