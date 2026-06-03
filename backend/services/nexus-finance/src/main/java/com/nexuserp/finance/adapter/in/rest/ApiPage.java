package com.nexuserp.finance.adapter.in.rest;

import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Réponse paginée standardisée — conforme à la convention API NexusERP.
 */
public record ApiPage<T>(
    List<T> data,
    Meta meta,
    Instant timestamp
) {
    public record Meta(int page, int size, long total, int totalPages) {}

    public static <T> ApiPage<T> of(Page<T> page) {
        return new ApiPage<>(
            page.getContent(),
            new Meta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()),
            Instant.now()
        );
    }
}
