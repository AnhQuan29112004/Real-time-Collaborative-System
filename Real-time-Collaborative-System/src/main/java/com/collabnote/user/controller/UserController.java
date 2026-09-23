package com.collabnote.user.controller;

import com.collabnote.common.response.ApiResponse;
import com.collabnote.user.dto.*;
import com.collabnote.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;
    @GetMapping
    public ApiResponse<UserResponse> me() { return ApiResponse.success(users.getCurrentUser()); }
    @PatchMapping
    public ApiResponse<UserResponse> update(@Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success(users.updateCurrentUser(request));
    }
}
