package com.collabnote.auth.repository;

import com.collabnote.auth.entity.AuthSessionEntity;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, UUID> {
    @Query("select s from AuthSessionEntity s where s.userId = :userId and s.revokedAt is null and s.expiresAt > :now order by s.createdAt desc")
    List<AuthSessionEntity> findActive(Long userId, Instant now);
    @Modifying
    @Query("update AuthSessionEntity s set s.revokedAt = :now where s.userId = :userId and s.revokedAt is null")
    int revokeAll(Long userId, Instant now);
}
