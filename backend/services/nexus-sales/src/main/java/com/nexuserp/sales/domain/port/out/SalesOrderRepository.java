package com.nexuserp.sales.domain.port.out;

import com.nexuserp.sales.domain.model.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepository {
    SalesOrder save(SalesOrder order);
    Optional<SalesOrder> findById(UUID id, String tenantId);
    Page<SalesOrder> findAll(String tenantId, String status, Pageable pageable);
}
