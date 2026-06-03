package com.nexuserp.production.adapter.in.rest;

import com.nexuserp.production.domain.model.WorkOrder;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkOrderDto(
    String id, String tenantId, String orderNumber,
    String productId, String productName,
    String status, String priority,
    BigDecimal quantityPlanned, BigDecimal quantityProduced, BigDecimal quantityRejected,
    LocalDate plannedStartDate, LocalDate plannedEndDate,
    String workcenter, String operator,
    boolean isLate, BigDecimal yieldRate
) {
    public static WorkOrderDto from(WorkOrder wo) {
        return new WorkOrderDto(
            wo.getId(), wo.getTenantId(), wo.getOrderNumber(),
            wo.getProductId(), wo.getProductName(),
            wo.getStatus().name(), wo.getPriority().name(),
            wo.getQuantityPlanned(), wo.getQuantityProduced(), wo.getQuantityRejected(),
            wo.getPlannedStartDate(), wo.getPlannedEndDate(),
            wo.getWorkcenter(), wo.getOperator(),
            wo.isLate(), wo.getYieldRate()
        );
    }
}
