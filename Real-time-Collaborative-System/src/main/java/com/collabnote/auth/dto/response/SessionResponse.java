package com.collabnote.auth.dto.response;
import java.time.Instant;
import java.util.UUID;
public record SessionResponse(UUID id, Instant createdAt, Instant expiresAt, boolean current) { }
