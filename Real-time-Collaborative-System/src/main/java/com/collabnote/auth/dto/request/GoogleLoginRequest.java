package com.collabnote.auth.dto.request;
import jakarta.validation.constraints.*;
public record GoogleLoginRequest(@NotBlank @Size(max = 8192) String idToken) { }
