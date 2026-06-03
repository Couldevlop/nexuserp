package com.nexuserp.hr.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveJpaRepository extends JpaRepository<LeaveJpaEntity, UUID> {
    Optional<LeaveJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
    Page<LeaveJpaEntity> findByEmployeeIdAndTenantId(UUID employeeId, String tenantId, Pageable pageable);
    Page<LeaveJpaEntity> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
}
