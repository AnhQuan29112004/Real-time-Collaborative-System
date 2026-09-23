package com.collabnote.common.config;

import com.collabnote.common.util.SecurityUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "currentAuditor", dateTimeProviderRef = "auditTime")
public class JpaAuditConfiguration {
    @Bean
    AuditorAware<Long> currentAuditor() {
        return () -> SecurityUtils.currentPrincipal().map(principal -> principal.userId());
    }

    @Bean
    DateTimeProvider auditTime(Clock clock) {
        return () -> Optional.of(Instant.now(clock));
    }
}
