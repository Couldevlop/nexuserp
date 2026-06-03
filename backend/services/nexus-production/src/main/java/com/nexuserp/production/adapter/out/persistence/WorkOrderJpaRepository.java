package com.nexuserp.production.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkOrderJpaRepository extends JpaRepository<WorkOrderJpaEntity, String> {
    Optional<WorkOrderJpaEntity> findByIdAndTenantId(String id, String tenantId);
    Page<WorkOrderJpaEntity> findByTenantId(String tenantId, Pageable pageable);
    Page<WorkOrderJpaEntity> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
}
