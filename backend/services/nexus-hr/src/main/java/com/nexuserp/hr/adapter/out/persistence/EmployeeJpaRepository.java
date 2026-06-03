package com.nexuserp.hr.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, UUID> {
    Optional<EmployeeJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
    Optional<EmployeeJpaEntity> findByEmployeeNumberAndTenantId(String employeeNumber, String tenantId);
    Page<EmployeeJpaEntity> findByTenantId(String tenantId, Pageable pageable);
    Page<EmployeeJpaEntity> findByTenantIdAndDepartment(String tenantId, String department, Pageable pageable);
    long countByTenantIdAndStatus(String tenantId, String status);
}
