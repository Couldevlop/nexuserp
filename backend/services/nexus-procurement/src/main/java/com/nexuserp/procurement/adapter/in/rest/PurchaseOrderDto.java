package com.nexuserp.procurement.adapter.in.rest;

import com.nexuserp.procurement.domain.model.PurchaseOrder;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseOrderDto(
    String id, String tenantId, String poNumber,
    String supplierId, String supplierName,
    String status, LocalDate expectedDeliveryDate,
    String currency, BigDecimal totalAmount,
    String notes, String approvedBy
) {
    public static PurchaseOrderDto from(PurchaseOrder o) {
        return new PurchaseOrderDto(
            o.getId(), o.getTenantId(), o.getOrderNumber(),
            o.getSupplierId(), o.getSupplierName(),
            o.getStatus().name(), o.getExpectedDeliveryDate(),
            o.getCurrency(),
            o.getTotalAmount() != null ? o.getTotalAmount().amount() : null,
            o.getNotes(), o.getApprovedBy()
        );
    }
}
