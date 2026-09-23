package com.collabnote.auth.dto.request;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token, @NotBlank @Size(min = 12, max = 72) String newPassword) { }
