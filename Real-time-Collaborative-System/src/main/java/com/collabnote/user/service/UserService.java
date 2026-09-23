package com.collabnote.user.service;

import com.collabnote.user.dto.*;
import java.util.Optional;

public interface UserService {
    UserResponse getCurrentUser();
    UserResponse updateCurrentUser(UpdateUserProfileRequest request);
    UserResponse getActiveUser(Long id);
    Optional<UserResponse> findByEmail(String email);
    UserResponse createUser(String email, String displayName, boolean verified);
    UserResponse lockActiveUser(Long id);
    void verifyEmail(Long id);
}
