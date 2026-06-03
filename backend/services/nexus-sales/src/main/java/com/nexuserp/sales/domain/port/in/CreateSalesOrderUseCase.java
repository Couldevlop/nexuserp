package com.nexuserp.sales.domain.port.in;

import com.nexuserp.sales.application.command.CreateSalesOrderCommand;
import com.nexuserp.sales.domain.model.SalesOrder;

import java.util.UUID;

public interface CreateSalesOrderUseCase {
    SalesOrder createSalesOrder(CreateSalesOrderCommand command);
    SalesOrder confirmSalesOrder(UUID id, String tenantId, String confirmedBy);
    SalesOrder cancelSalesOrder(UUID id, String tenantId, String reason);
}
