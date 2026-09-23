package com.collabnote.user.dto;

import jakarta.validation.constraints.*;

public record UpdateUserProfileRequest(@NotBlank @Size(max = 100) String displayName,
                                       @NotNull @PositiveOrZero Long version) { }
