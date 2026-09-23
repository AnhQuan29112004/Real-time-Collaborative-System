package com.collabnote.common.query;

import com.collabnote.common.exception.BusinessException;
import com.collabnote.common.exception.ErrorCode;
import java.util.Collection;
import java.util.List;

public record SearchCriteria(String key, SearchOperation operation, Object value, boolean orPredicate) {
    public SearchCriteria {
        if (key == null || !key.matches("[A-Za-z][A-Za-z0-9_]*") || operation == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (value instanceof Collection<?> values) {
            if (values.size() > 500 || values.stream().anyMatch(item -> item == null)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            value = List.copyOf(values);
        }
        if (value instanceof String text && text.length() > 1000) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    public SearchCriteria(String key, SearchOperation operation, Object value) {
        this(key, operation, value, false);
    }
}
