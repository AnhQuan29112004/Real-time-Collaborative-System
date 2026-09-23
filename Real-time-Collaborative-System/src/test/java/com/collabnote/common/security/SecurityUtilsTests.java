package com.collabnote.common.security;

import com.collabnote.common.exception.BusinessException;
import com.collabnote.common.util.SecurityUtils;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.*;

class SecurityUtilsTests {
    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void identityIsTakenOnlyFromVerifiedTypedPrincipal() {
        var principal = new CollabPrincipal(42L, "alice@example.test");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        assertThat(SecurityUtils.requireCurrentUserId()).isEqualTo(42L);
        assertThat(SecurityUtils.getCurrentUserEmail()).isEqualTo("alice@example.test");
    }

    @Test
    void numericUsernameIsNotMistakenForUserId() {
        var user = User.withUsername("42").password("unused").roles("USER").build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        assertThat(SecurityUtils.currentPrincipal()).isEmpty();
        assertThatThrownBy(SecurityUtils::requireCurrentUserId).isInstanceOf(BusinessException.class);
    }

    @Test
    void anonymousAndUnverifiedTokensDoNotBecomeAuditors() {
        var principal = new CollabPrincipal(42L, "alice@example.test");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated(principal, null));
        assertThat(SecurityUtils.currentPrincipal()).isEmpty();
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "test", principal, List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertThat(SecurityUtils.currentPrincipal()).isEmpty();
    }
}
