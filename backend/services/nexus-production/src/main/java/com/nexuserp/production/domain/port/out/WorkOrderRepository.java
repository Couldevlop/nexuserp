package com.nexuserp.production.domain.port.out;

import com.nexuserp.production.domain.model.WorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository {
    WorkOrder save(WorkOrder workOrder);
    Optional<WorkOrder> findById(String id, String tenantId);
    Page<WorkOrder> findAll(String tenantId, String status, Pageable pageable);
}
