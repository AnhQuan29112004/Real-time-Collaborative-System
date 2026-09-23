package com.collabnote.auth.service;
import com.collabnote.auth.dto.*;
import com.collabnote.auth.dto.request.*;
import com.collabnote.user.dto.UserResponse;
public interface CredentialService {
    UserResponse register(RegisterRequest request);
    IssuedTokens login(LoginRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
    void resetPassword(Long userId, String password);
    boolean hasLocalIdentity(Long userId);
}
