package com.collabnote.auth.dto;
import com.collabnote.user.dto.UserResponse;
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) { }
