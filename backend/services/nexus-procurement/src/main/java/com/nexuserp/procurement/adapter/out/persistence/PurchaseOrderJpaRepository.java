package com.nexuserp.procurement.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderJpaRepository extends JpaRepository<PurchaseOrderJpaEntity, String> {
    Optional<PurchaseOrderJpaEntity> findByIdAndTenantId(String id, String tenantId);
    Page<PurchaseOrderJpaEntity> findByTenantId(String tenantId, Pageable pageable);
    Page<PurchaseOrderJpaEntity> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
}
