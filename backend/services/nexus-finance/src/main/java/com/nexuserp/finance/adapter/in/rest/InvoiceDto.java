package com.nexuserp.finance.adapter.in.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceDto(
    UUID id,
    String invoiceNumber,
    String type,
    String status,
    UUID partnerId,
    String partnerName,
    String partnerVat,
    LocalDate invoiceDate,
    LocalDate dueDate,
    String currency,
    BigDecimal subtotal,
    BigDecimal taxAmount,
    BigDecimal total,
    BigDecimal amountPaid,
    BigDecimal amountDue,
    String notes,
    List<InvoiceLineDto> lines
) {
    public record InvoiceLineDto(
        UUID id,
        int lineNumber,
        String description,
        String productCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPct,
        BigDecimal taxRate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total
    ) {}
}
