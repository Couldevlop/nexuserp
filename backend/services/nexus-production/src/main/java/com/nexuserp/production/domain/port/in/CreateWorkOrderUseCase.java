package com.nexuserp.production.domain.port.in;

import com.nexuserp.production.application.command.CreateWorkOrderCommand;
import com.nexuserp.production.domain.model.WorkOrder;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreateWorkOrderUseCase {
    WorkOrder createWorkOrder(CreateWorkOrderCommand command);
    WorkOrder releaseWorkOrder(UUID id, String tenantId);
    WorkOrder startWorkOrder(UUID id, String tenantId, String operatorId);
    WorkOrder recordProduction(UUID id, String tenantId, BigDecimal quantity, BigDecimal rejected, String operatorId);
}
