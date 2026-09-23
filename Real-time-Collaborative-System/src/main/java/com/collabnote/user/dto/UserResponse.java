package com.collabnote.user.dto;

import java.time.Instant;

public record UserResponse(Long id, String email, String displayName, Instant emailVerifiedAt,
                           Instant disabledAt, long version, Instant createdAt) { }
