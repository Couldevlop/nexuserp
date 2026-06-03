package com.nexuserp.hr.application.query;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record EmployeePageQuery(
    String tenantId,
    String department,
    int page,
    int size,
    String sortBy,
    String sortDir
) {
    public Pageable toPageable() {
        Sort sort = "desc".equalsIgnoreCase(sortDir)
            ? Sort.by(sortBy != null ? sortBy : "lastName").descending()
            : Sort.by(sortBy != null ? sortBy : "lastName").ascending();
        return PageRequest.of(page, size, sort);
    }
}
