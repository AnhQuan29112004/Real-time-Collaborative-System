package com.collabnote.auth.repository;

import com.collabnote.auth.entity.AuthRefreshTokenEntity;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshTokenEntity, UUID> {
    Optional<AuthRefreshTokenEntity> findByTokenHash(String hash);
    @Query("select s.userId from AuthRefreshTokenEntity t, AuthSessionEntity s where t.sessionId = s.id and t.tokenHash = :hash")
    Optional<Long> findOwnerByHash(String hash);
}
