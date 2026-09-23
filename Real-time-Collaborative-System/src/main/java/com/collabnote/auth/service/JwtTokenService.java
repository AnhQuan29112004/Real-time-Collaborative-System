package com.collabnote.auth.service;
import com.collabnote.auth.dto.*;
import com.collabnote.user.dto.UserResponse;
import java.time.Instant;
import java.util.UUID;
public interface JwtTokenService {
    AuthResponse issue(UserResponse user, UUID sessionId, Instant sessionExpiresAt);
}
