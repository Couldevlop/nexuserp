package com.nexuserp.reporting.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, String> {
    Optional<ReportJpaEntity> findByIdAndTenantId(String id, String tenantId);
}
