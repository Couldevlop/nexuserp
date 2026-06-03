package com.nexuserp.sales.domain.port.in;

import com.nexuserp.sales.domain.model.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GetSalesOrderUseCase {
    SalesOrder getById(UUID id, String tenantId);
    Page<SalesOrder> list(String tenantId, String status, Pageable pageable);
}
