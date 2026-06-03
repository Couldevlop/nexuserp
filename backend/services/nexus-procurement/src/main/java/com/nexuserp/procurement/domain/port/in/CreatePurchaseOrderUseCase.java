package com.nexuserp.procurement.domain.port.in;

import com.nexuserp.procurement.application.command.CreatePurchaseOrderCommand;
import com.nexuserp.procurement.domain.model.PurchaseOrder;

public interface CreatePurchaseOrderUseCase {
    PurchaseOrder createPurchaseOrder(CreatePurchaseOrderCommand command);
    PurchaseOrder approvePurchaseOrder(java.util.UUID id, String tenantId, String approvedBy);
}
