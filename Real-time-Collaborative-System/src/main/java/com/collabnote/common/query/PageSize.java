package com.collabnote.common.query;

import com.collabnote.common.exception.BusinessException;
import com.collabnote.common.exception.ErrorCode;

public final class PageSize {
    public static final int DEFAULT = 20;
    public static final int MAX = 100;

    private PageSize() { }

    public static int requireValid(Integer size) {
        if (size == null) {
            return DEFAULT;
        }
        if (size < 1 || size > MAX) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return size;
    }
}
