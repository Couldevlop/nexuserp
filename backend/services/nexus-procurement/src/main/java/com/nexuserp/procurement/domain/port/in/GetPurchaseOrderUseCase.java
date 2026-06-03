package com.nexuserp.procurement.domain.port.in;

import com.nexuserp.procurement.domain.model.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GetPurchaseOrderUseCase {
    PurchaseOrder getById(UUID id, String tenantId);
    Page<PurchaseOrder> list(String tenantId, String status, Pageable pageable);
}
