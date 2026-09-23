package com.collabnote.auth.repository;

import com.collabnote.auth.entity.AuthIdentityEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuthIdentityRepository extends JpaRepository<AuthIdentityEntity, Long> {
    @Query("select i.userId from AuthIdentityEntity i where i.provider = :provider and i.subject = :subject")
    Optional<Long> findOwner(String provider, String subject);
    Optional<AuthIdentityEntity> findByProviderAndSubject(String provider, String subject);
    Optional<AuthIdentityEntity> findByUserIdAndProvider(Long userId, String provider);
}
