package com.nexuserp.payment.application.query;

import com.nexuserp.payment.domain.model.PaymentStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PaymentPageQuery(
    String tenantId,
    PaymentStatus status,
    int page,
    int size,
    String sortBy,
    String sortDir
) {
    public Pageable toPageable() {
        String field = sortBy != null ? sortBy : "createdAt";
        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("asc")
            ? Sort.by(field).ascending()
            : Sort.by(field).descending();
        return PageRequest.of(page, Math.min(size, 100), sort);
    }
}
