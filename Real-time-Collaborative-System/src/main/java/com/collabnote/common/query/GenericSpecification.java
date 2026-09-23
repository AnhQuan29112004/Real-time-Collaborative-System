package com.collabnote.common.query;

import com.collabnote.common.exception.BusinessException;
import com.collabnote.common.exception.ErrorCode;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.ClassUtils;

/** Internal filter adapter. Use SpecificationBuilder to enforce a field allowlist and required scope. */
final class GenericSpecification<T> implements Specification<T> {
    private final SearchCriteria criteria;

    GenericSpecification(SearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Path<?> path = root.get(criteria.key());
        Object value = criteria.value();
        return switch (criteria.operation()) {
            case EQUALITY -> value == null ? builder.isNull(path) : builder.equal(path, checked(path, value));
            case NEGATION -> value == null ? builder.isNotNull(path) : builder.notEqual(path, checked(path, value));
            case GREATER_THAN, LESS_THAN -> compare(path, builder);
            case LIKE, STARTS_WITH, ENDS_WITH -> text(path, builder);
            case IN, NOT_IN -> membership(path, builder);
        };
    }

    private Object checked(Path<?> path, Object value) {
        if (value == null || !ClassUtils.resolvePrimitiveIfNecessary(path.getJavaType()).isInstance(value)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate compare(Path<?> path, CriteriaBuilder builder) {
        Object value = checked(path, criteria.value());
        if (!(value instanceof Comparable comparable)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        // Runtime type is checked above; never convert numbers/dates into strings for comparison.
        return criteria.operation() == SearchOperation.GREATER_THAN
                ? builder.greaterThan((Path) path, comparable)
                : builder.lessThan((Path) path, comparable);
    }

    private Predicate text(Path<?> path, CriteriaBuilder builder) {
        if (path.getJavaType() != String.class || !(criteria.value() instanceof String value)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        String escaped = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        String pattern = switch (criteria.operation()) {
            case STARTS_WITH -> escaped + "%";
            case ENDS_WITH -> "%" + escaped;
            default -> "%" + escaped + "%";
        };
        return builder.like(path.as(String.class), pattern, '\\');
    }

    private Predicate membership(Path<?> path, CriteriaBuilder builder) {
        if (!(criteria.value() instanceof Collection<?> values)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        values.forEach(value -> checked(path, value));
        Predicate predicate = values.isEmpty() ? builder.disjunction() : path.in(values);
        return criteria.operation() == SearchOperation.NOT_IN ? builder.not(predicate) : predicate;
    }
}
