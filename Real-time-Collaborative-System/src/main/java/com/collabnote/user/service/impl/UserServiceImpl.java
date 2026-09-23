package com.collabnote.user.service.impl;

import com.collabnote.common.exception.*;
import com.collabnote.common.util.SecurityUtils;
import com.collabnote.user.dto.*;
import com.collabnote.user.entity.UserEntity;
import com.collabnote.user.mapper.UserMapper;
import com.collabnote.user.repository.UserRepository;
import com.collabnote.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    private final UserMapper mapper;
    private final Clock clock;
    private final jakarta.persistence.EntityManager entityManager;

    public UserResponse getCurrentUser() { return getActiveUser(SecurityUtils.requireCurrentUserId()); }

    public UserResponse getActiveUser(Long id) {
        return mapper.toResponse(active(users.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED))));
    }

    public Optional<UserResponse> findByEmail(String email) { return users.findByEmail(email).map(mapper::toResponse); }

    @Transactional
    public UserResponse createUser(String email, String displayName, boolean verified) {
        return mapper.toResponse(users.saveAndFlush(new UserEntity(email, displayName, verified ? Instant.now(clock) : null)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UserResponse lockActiveUser(Long id) {
        var user = users.findLockedById(id).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        entityManager.refresh(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return mapper.toResponse(active(user));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void verifyEmail(Long id) {
        active(users.findLockedById(id).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED)))
                .verifyEmail(Instant.now(clock));
    }

    @Transactional
    public UserResponse updateCurrentUser(UpdateUserProfileRequest request) {
        var user = active(users.findLockedById(SecurityUtils.requireCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED)));
        if (user.getVersion() != request.version()) throw new BusinessException(ErrorCode.CONFLICT);
        user.updateProfile(request.displayName().strip());
        users.flush();
        return mapper.toResponse(user);
    }

    private UserEntity active(UserEntity user) {
        if (user.getDisabledAt() != null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return user;
    }
}
