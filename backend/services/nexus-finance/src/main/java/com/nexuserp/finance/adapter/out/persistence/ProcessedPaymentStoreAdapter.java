package com.nexuserp.finance.adapter.out.persistence;

import com.nexuserp.finance.domain.port.out.ProcessedPaymentStore;
import org.springframework.stereotype.Repository;

@Repository
public class ProcessedPaymentStoreAdapter implements ProcessedPaymentStore {

    private final ProcessedPaymentJpaRepository jpaRepository;

    public ProcessedPaymentStoreAdapter(ProcessedPaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean isProcessed(String paymentId, String tenantId) {
        return jpaRepository.existsByTenantIdAndPaymentId(tenantId, paymentId);
    }

    @Override
    public void markProcessed(String paymentId, String tenantId, String invoiceId) {
        jpaRepository.save(new ProcessedPaymentJpaEntity(tenantId, paymentId, invoiceId));
    }
}
