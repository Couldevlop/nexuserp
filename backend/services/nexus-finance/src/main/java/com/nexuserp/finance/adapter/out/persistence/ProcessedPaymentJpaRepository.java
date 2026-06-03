package com.nexuserp.finance.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedPaymentJpaRepository extends JpaRepository<ProcessedPaymentJpaEntity, String> {
    boolean existsByTenantIdAndPaymentId(String tenantId, String paymentId);
}
