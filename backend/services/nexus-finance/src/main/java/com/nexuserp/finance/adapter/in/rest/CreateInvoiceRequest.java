package com.nexuserp.finance.adapter.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateInvoiceRequest(
    @NotBlank String invoiceType,
    UUID partnerId,
    String partnerName,
    String partnerVat,
    @NotNull LocalDate invoiceDate,
    LocalDate dueDate,
    @Size(min = 3, max = 3) String currency,
    String notes,
    @NotEmpty @Valid List<LineRequest> lines
) {
    public record LineRequest(
        @NotBlank String description,
        String productCode,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @PositiveOrZero BigDecimal unitPrice,
        @DecimalMin("0") @DecimalMax("100") BigDecimal discountPct,
        @DecimalMin("0") @DecimalMax("100") BigDecimal taxRate,
        UUID accountId,
        UUID costCenterId
    ) {}
}
