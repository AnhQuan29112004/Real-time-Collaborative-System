package com.collabnote.auth.dto.request;
import jakarta.validation.constraints.*;
public record ChangePasswordRequest(@NotBlank @Size(max = 72) String currentPassword, @NotBlank @Size(min = 12, max = 72) String newPassword) { }
