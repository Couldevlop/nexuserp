package com.nexuserp.procurement.domain.port.out;

import com.nexuserp.procurement.domain.model.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PurchaseOrderRepository {
    PurchaseOrder save(PurchaseOrder order);
    Optional<PurchaseOrder> findById(String id, String tenantId);
    Page<PurchaseOrder> findAll(String tenantId, String status, Pageable pageable);
}
