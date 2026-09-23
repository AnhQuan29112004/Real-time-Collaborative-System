package com.collabnote.common.query;

import com.collabnote.common.config.ClockConfiguration;
import com.collabnote.common.config.JpaAuditConfiguration;
import com.collabnote.common.exception.BusinessException;
import com.collabnote.common.security.CollabPrincipal;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;
import static com.collabnote.common.query.SearchOperation.*;

@DataJpaTest(properties = "spring.config.import=")
@ActiveProfiles("test")
@Import({ClockConfiguration.class, JpaAuditConfiguration.class})
class SpecificationBuilderTests {
    @Autowired EntityManager em;

    @BeforeEach
    void seed() {
        em.persist(new QuerySample(1, 10, "100%_done", 2));
        em.persist(new QuerySample(2, 10, "other", 10));
        em.persist(new QuerySample(3, 20, "other", 100));
        em.flush();
    }

    @AfterEach
    void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void numericComparisonUsesNumericType() {
        assertThat(find(builder().with("score", GREATER_THAN, 3).build()))
                .extracting(row -> row.id).containsExactly(2L);
    }

    @Test
    void orFiltersCannotEscapeWorkspaceScope() {
        assertThat(find(builder().with("title", EQUALITY, "missing")
                .with("score", GREATER_THAN, 5, true).build()))
                .extracting(row -> row.id).containsExactly(2L);
    }

    @Test
    void emptyFilterStillEnforcesScope() {
        assertThat(find(builder().build())).extracting(row -> row.id).containsExactly(1L, 2L);
    }

    @Test
    void likeTreatsPercentAndUnderscoreAsLiteralText() {
        assertThat(find(builder().with("title", LIKE, "%_").build()))
                .extracting(row -> row.id).containsExactly(1L);
    }

    @Test
    void inSupportsCollectionsAndEmptySets() {
        assertThat(find(builder().with("score", IN, List.of(2, 100)).build()))
                .extracting(row -> row.id).containsExactly(1L);
        assertThat(find(builder().with("score", IN, List.of()).build())).isEmpty();
        assertThat(find(builder().with("score", NOT_IN, List.of()).build())).hasSize(2);
    }

    @Test
    void rejectsUnknownFieldsWrongTypesAndUnscopedQueries() {
        assertThatThrownBy(() -> builder().with("workspaceId", EQUALITY, 20L))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> builder().with("title.length", EQUALITY, 1))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> find(builder().with("score", GREATER_THAN, "3").build()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new SpecificationBuilder<QuerySample>(null, Set.of("title")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void limitsPageSizeAndFilterCount() {
        assertThat(PageSize.requireValid(null)).isEqualTo(20);
        assertThatThrownBy(() -> PageSize.requireValid(101)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> PageSize.requireValid(0)).isInstanceOf(BusinessException.class);
        var query = builder();
        for (int i = 0; i < 20; i++) query.with("score", GREATER_THAN, 0);
        assertThatThrownBy(() -> query.with("score", GREATER_THAN, 0)).isInstanceOf(BusinessException.class);
    }

    @Test
    void auditingUsesVerifiedPrincipalAndUtcTime() {
        var principal = new CollabPrincipal(42L, "alice@example.test");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        var row = new QuerySample(4, 10, "audited", 1);
        em.persist(row);
        em.flush();
        assertThat(row.getCreatedBy()).isEqualTo(42L);
        assertThat(row.getCreatedAt()).isNotNull();
        assertThat(row.getUpdatedAt()).isEqualTo(row.getCreatedAt());
    }

    private SpecificationBuilder<QuerySample> builder() {
        Specification<QuerySample> scope = (root, query, cb) -> cb.equal(root.get("workspaceId"), 10L);
        return new SpecificationBuilder<>(scope, Set.of("title", "score"));
    }

    private List<QuerySample> find(Specification<QuerySample> spec) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(QuerySample.class);
        var root = query.from(QuerySample.class);
        query.where(spec.toPredicate(root, query, cb)).orderBy(cb.asc(root.get("id")));
        return em.createQuery(query).getResultList();
    }
}
