package com.collabnote.auth.service;
import com.collabnote.auth.dto.IssuedTokens;
import com.collabnote.auth.dto.response.SessionResponse;
import com.collabnote.user.dto.UserResponse;
import java.util.*;
public interface AuthSessionService {
    IssuedTokens issue(UserResponse user);
    IssuedTokens refresh(String refreshToken);
    UserResponse authenticate(UUID sessionId, Long userId);
    void logout(String refreshToken);
    void revoke(Long userId, UUID sessionId);
    void revokeAll(Long userId);
    List<SessionResponse> list(Long userId, UUID currentSession);
}
