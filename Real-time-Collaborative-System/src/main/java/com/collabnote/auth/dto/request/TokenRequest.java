package com.collabnote.auth.dto.request;
import jakarta.validation.constraints.*;
public record TokenRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token) { }
