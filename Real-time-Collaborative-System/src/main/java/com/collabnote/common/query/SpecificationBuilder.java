package com.collabnote.common.query;

import com.collabnote.common.exception.BusinessException;
import com.collabnote.common.exception.ErrorCode;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

/** Request-scoped builder: mandatory scope AND (user filters); OR can never escape the scope. */
public final class SpecificationBuilder<T> {
    private static final int MAX_FILTERS = 20;
    private final Specification<T> requiredScope;
    private final Set<String> allowedFields;
    private final List<SearchCriteria> filters = new ArrayList<>();

    public SpecificationBuilder(Specification<T> requiredScope, Set<String> allowedFields) {
        this.requiredScope = Objects.requireNonNull(requiredScope, "requiredScope");
        this.allowedFields = Set.copyOf(allowedFields);
    }

    public SpecificationBuilder<T> with(String key, SearchOperation operation, Object value) {
        return with(key, operation, value, false);
    }

    public SpecificationBuilder<T> with(String key, SearchOperation operation, Object value, boolean or) {
        if (key == null || !allowedFields.contains(key) || filters.size() >= MAX_FILTERS) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        filters.add(new SearchCriteria(key, operation, value, or));
        return this;
    }

    public Specification<T> build() {
        List<SearchCriteria> snapshot = List.copyOf(filters);
        return (root, query, builder) -> {
            Predicate scope = Objects.requireNonNull(requiredScope.toPredicate(root, query, builder),
                    "Required scope must return a predicate, not an unrestricted specification");
            Predicate userFilter = null;
            for (SearchCriteria criteria : snapshot) {
                Predicate next = new GenericSpecification<T>(criteria).toPredicate(root, query, builder);
                userFilter = userFilter == null ? next : criteria.orPredicate()
                        ? builder.or(userFilter, next) : builder.and(userFilter, next);
            }
            return userFilter == null ? scope : builder.and(scope, userFilter);
        };
    }
}
