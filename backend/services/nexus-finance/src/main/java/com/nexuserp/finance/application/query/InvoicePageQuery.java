package com.nexuserp.finance.application.query;

import com.nexuserp.finance.domain.model.Invoice;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

public record InvoicePageQuery(
    String tenantId,
    Invoice.InvoiceStatus status,
    LocalDate dateFrom,
    LocalDate dateTo,
    String partnerName,
    int page,
    int size,
    String sortBy,
    String sortDir
) {
    public Pageable toPageable() {
        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("asc")
            ? Sort.by(sortBy != null ? sortBy : "invoiceDate").ascending()
            : Sort.by(sortBy != null ? sortBy : "invoiceDate").descending();
        return PageRequest.of(page, Math.min(size, 100), sort);
    }
}
