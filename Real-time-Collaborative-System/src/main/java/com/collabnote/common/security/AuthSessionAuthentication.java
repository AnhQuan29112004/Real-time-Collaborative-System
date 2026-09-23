package com.collabnote.common.security;
import java.util.*;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
public class AuthSessionAuthentication extends AbstractAuthenticationToken {
    private final CollabPrincipal principal;
    private final UUID sessionId;
    public AuthSessionAuthentication(CollabPrincipal principal, UUID sessionId) {
        super(List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.principal = principal; this.sessionId = sessionId; super.setAuthenticated(true);
    }
    @Override public Object getCredentials() { return ""; }
    @Override public CollabPrincipal getPrincipal() { return principal; }
    public UUID getSessionId() { return sessionId; }
}
