package com.collabnote.auth.service;
import com.collabnote.auth.dto.*;
import com.collabnote.auth.dto.request.*;
public interface AuthService {
    void register(RegisterRequest request);
    IssuedTokens login(LoginRequest request);
    void requestVerification(String email);
    void verifyEmail(String token);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}
