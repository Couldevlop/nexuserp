package com.nexuserp.inventory.application.query;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record ProductPageQuery(
    String tenantId,
    String category,
    int page,
    int size,
    String sortBy,
    String sortDir
) {
    public Pageable toPageable() {
        Sort sort = "desc".equalsIgnoreCase(sortDir)
            ? Sort.by(sortBy != null ? sortBy : "name").descending()
            : Sort.by(sortBy != null ? sortBy : "name").ascending();
        return PageRequest.of(page, size, sort);
    }
}
