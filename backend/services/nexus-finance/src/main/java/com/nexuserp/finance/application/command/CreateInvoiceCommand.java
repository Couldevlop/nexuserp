package com.nexuserp.finance.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Commande de création de facture.
 * Record immuable — DTO command.
 */
public record CreateInvoiceCommand(
    String tenantId,
    String invoiceType,      // CUSTOMER, SUPPLIER, CREDIT_NOTE, DEBIT_NOTE
    UUID partnerId,
    String partnerName,
    String partnerVat,
    LocalDate invoiceDate,
    LocalDate dueDate,
    String currency,
    String notes,
    String createdBy,
    List<LineCommand> lines
) {
    public record LineCommand(
        String description,
        String productCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPct,
        BigDecimal taxRate,
        UUID accountId,
        UUID costCenterId
    ) {}
}
