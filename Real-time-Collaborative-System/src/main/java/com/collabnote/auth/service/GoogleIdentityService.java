package com.collabnote.auth.service;
import com.collabnote.auth.dto.IssuedTokens;
public interface GoogleIdentityService {
    IssuedTokens login(String idToken);
    void link(Long userId, String idToken);
}
