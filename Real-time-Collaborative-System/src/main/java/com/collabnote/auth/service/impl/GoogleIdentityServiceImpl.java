package com.collabnote.auth.service.impl;

import com.collabnote.auth.dto.IssuedTokens;
import com.collabnote.auth.service.*;
import com.collabnote.common.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleIdentityServiceImpl implements GoogleIdentityService {
    private final GoogleTokenService tokens;
    private final GoogleAccountService accounts;
    public IssuedTokens login(String rawToken) {
        var identity = tokens.verify(rawToken);
        try { return accounts.login(identity); }
        catch (DataIntegrityViolationException ex) { throw new BusinessException(ErrorCode.ACCOUNT_LINK_REQUIRED); }
    }
    public void link(Long userId, String rawToken) {
        var identity = tokens.verify(rawToken);
        try { accounts.link(userId, identity); }
        catch (DataIntegrityViolationException ex) { throw new BusinessException(ErrorCode.ACCOUNT_LINK_REQUIRED); }
    }
}
