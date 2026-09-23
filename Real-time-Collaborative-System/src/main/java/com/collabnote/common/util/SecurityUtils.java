package com.collabnote.common.util;

import com.collabnote.common.exception.BusinessException;
import com.collabnote.common.exception.ErrorCode;
import com.collabnote.common.security.CollabPrincipal;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() { }

    public static Optional<CollabPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return auth.getPrincipal() instanceof CollabPrincipal principal
                ? Optional.of(principal) : Optional.empty();
    }

    public static Long requireCurrentUserId() {
        return currentPrincipal().map(CollabPrincipal::userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    public static Long getCurrentUserId() {
        return currentPrincipal().map(CollabPrincipal::userId).orElse(null);
    }

    public static String getCurrentUserEmail() {
        return currentPrincipal().map(CollabPrincipal::email).orElse(null);
    }
}
