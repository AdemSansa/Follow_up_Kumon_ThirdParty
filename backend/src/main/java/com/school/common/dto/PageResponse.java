package com.school.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Explicit page envelope instead of returning Spring Data's Page directly —
 * keeps the wire format stable regardless of Jackson/Spring Data version
 * changes.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
