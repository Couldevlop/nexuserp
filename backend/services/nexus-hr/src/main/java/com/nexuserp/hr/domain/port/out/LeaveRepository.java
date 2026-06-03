package com.nexuserp.hr.domain.port.out;

import com.nexuserp.hr.domain.model.Leave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface LeaveRepository {
    Leave save(Leave leave);
    Optional<Leave> findById(UUID id, String tenantId);
    Page<Leave> findByEmployee(UUID employeeId, String tenantId, Pageable pageable);
    Page<Leave> findPending(String tenantId, Pageable pageable);
}
