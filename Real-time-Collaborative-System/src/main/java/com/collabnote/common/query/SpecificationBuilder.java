package com.collabnote.common.query;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SpecificationBuilder<T> {

    private final List<SearchCriteria> params;

    public SpecificationBuilder() {
        this.params = new ArrayList<>();
    }

    public final SpecificationBuilder<T> with(String key, SearchOperation operation, Object value) {
        params.add(new SearchCriteria(key, operation, value));
        return this;
    }

    public final SpecificationBuilder<T> with(String key, SearchOperation operation, Object value, boolean isOr) {
        params.add(new SearchCriteria(key, operation, value, isOr));
        return this;
    }

    public Specification<T> build() {
        if (params.isEmpty()) {
            return null;
        }

        Specification<T> result = new GenericSpecification<>(params.get(0));

        for (int i = 1; i < params.size(); i++) {
            SearchCriteria criteria = params.get(i);
            result = criteria.isOrPredicate()
                    ? Specification.where(result).or(new GenericSpecification<>(criteria))
                    : Specification.where(result).and(new GenericSpecification<>(criteria));
        }

        return result;
    }
}
