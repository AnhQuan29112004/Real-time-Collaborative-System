package com.collabnote.common.response;

import java.util.List;
import org.springframework.data.domain.Page;

/** Use only when total counts are required; document lists should use CursorPageResponse. */
public record PageResponse<T>(List<T> content, long totalElements, int totalPages, boolean hasNext) {
    public PageResponse {
        content = List.copyOf(content);
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }
}
