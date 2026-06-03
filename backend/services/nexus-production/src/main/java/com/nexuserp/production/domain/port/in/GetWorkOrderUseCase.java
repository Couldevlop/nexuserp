package com.nexuserp.production.domain.port.in;

import com.nexuserp.production.domain.model.WorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GetWorkOrderUseCase {
    WorkOrder getById(UUID id, String tenantId);
    Page<WorkOrder> list(String tenantId, String status, Pageable pageable);
}
