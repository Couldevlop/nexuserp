package com.nexuserp.procurement.application.command;

import com.nexuserp.procurement.domain.model.PurchaseOrderLine;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePurchaseOrderCommand(
    String tenantId,
    UUID supplierId,
    String supplierName,
    LocalDate expectedDeliveryDate,
    String currency,
    String notes,
    List<PurchaseOrderLine.LineData> lines,
    String createdBy
) {}
