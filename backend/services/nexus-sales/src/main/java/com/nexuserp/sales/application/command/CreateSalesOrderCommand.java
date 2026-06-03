package com.nexuserp.sales.application.command;

import com.nexuserp.sales.domain.model.SalesOrderLine;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateSalesOrderCommand(
    String tenantId,
    UUID customerId,
    String customerName,
    String customerRef,
    LocalDate requestedDeliveryDate,
    String currency,
    String shippingAddress,
    String notes,
    List<SalesOrderLine.LineData> lines,
    String createdBy
) {}
