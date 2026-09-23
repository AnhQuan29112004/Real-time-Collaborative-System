package com.collabnote.auth.repository;

import com.collabnote.auth.entity.AccountTokenEntity;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AccountTokenRepository extends JpaRepository<AccountTokenEntity, UUID> {
    @Query("select t.userId from AccountTokenEntity t where t.tokenHash = :hash and t.purpose = :purpose")
    Optional<Long> findOwner(String hash, String purpose);
    Optional<AccountTokenEntity> findByTokenHashAndPurpose(String hash, String purpose);
    Optional<AccountTokenEntity> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(Long userId, String purpose);
    @Modifying
    @Query("update AccountTokenEntity t set t.consumedAt = :now where t.userId = :userId and t.purpose = :purpose and t.consumedAt is null")
    int invalidate(Long userId, String purpose, Instant now);
}
