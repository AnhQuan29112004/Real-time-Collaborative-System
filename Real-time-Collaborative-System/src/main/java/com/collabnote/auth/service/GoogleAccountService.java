package com.collabnote.auth.service;
import com.collabnote.auth.dto.*;
public interface GoogleAccountService {
    IssuedTokens login(GoogleIdentity identity);
    void link(Long userId, GoogleIdentity identity);
}
