package com.nexuserp.production.application.command;

import com.nexuserp.production.domain.model.WorkOrder;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateWorkOrderCommand(
    String tenantId,
    String productId,
    String productName,
    BigDecimal quantityPlanned,
    LocalDate plannedStartDate,
    LocalDate plannedEndDate,
    WorkOrder.Priority priority,
    String workcenter,
    String bomId,
    String routingId,
    String notes,
    String createdBy
) {}
