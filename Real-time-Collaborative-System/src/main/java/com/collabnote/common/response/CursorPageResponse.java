package com.collabnote.common.response;

import java.util.List;

/** Cursor encoding and the stable ordering belong to the module's query adapter. */
public record CursorPageResponse<T>(List<T> content, String nextCursor, boolean hasNext) {
    public CursorPageResponse {
        content = List.copyOf(content);
        if (hasNext != (nextCursor != null && !nextCursor.isBlank())) {
            throw new IllegalArgumentException("hasNext and nextCursor must agree");
        }
    }
}
