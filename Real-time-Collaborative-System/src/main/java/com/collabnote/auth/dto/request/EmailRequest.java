package com.collabnote.auth.dto.request;
import jakarta.validation.constraints.*;
public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) { }
