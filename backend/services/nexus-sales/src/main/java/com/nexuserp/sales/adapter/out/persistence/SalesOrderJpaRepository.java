package com.nexuserp.sales.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesOrderJpaRepository extends JpaRepository<SalesOrderJpaEntity, UUID> {
    Optional<SalesOrderJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
    Page<SalesOrderJpaEntity> findByTenantId(String tenantId, Pageable pageable);
    Page<SalesOrderJpaEntity> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
}
