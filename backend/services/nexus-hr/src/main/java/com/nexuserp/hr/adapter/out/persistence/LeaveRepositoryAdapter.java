package com.nexuserp.hr.adapter.out.persistence;

import com.nexuserp.hr.domain.model.Leave;
import com.nexuserp.hr.domain.port.out.LeaveRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class LeaveRepositoryAdapter implements LeaveRepository {

    private final LeaveJpaRepository jpaRepository;

    public LeaveRepositoryAdapter(LeaveJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Leave save(Leave leave) {
        return toDomain(jpaRepository.save(toJpa(leave)));
    }

    @Override
    public Optional<Leave> findById(UUID id, String tenantId) {
        return jpaRepository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public Page<Leave> findByEmployee(UUID employeeId, String tenantId, Pageable pageable) {
        return jpaRepository.findByEmployeeIdAndTenantId(employeeId, tenantId, pageable).map(this::toDomain);
    }

    @Override
    public Page<Leave> findPending(String tenantId, Pageable pageable) {
        return jpaRepository.findByTenantIdAndStatus(tenantId, "SUBMITTED", pageable).map(this::toDomain);
    }

    private LeaveJpaEntity toJpa(Leave l) {
        LeaveJpaEntity e = new LeaveJpaEntity();
        e.setId(l.getId());
        e.setTenantId(l.getTenantId().value());
        e.setEmployeeId(l.getEmployeeId());
        e.setLeaveType(l.getLeaveType().name());
        e.setStatus(l.getStatus().name());
        e.setStartDate(l.getStartDate());
        e.setEndDate(l.getEndDate());
        e.setDurationDays(l.getDurationDays());
        e.setReason(l.getReason());
        e.setApprovedBy(l.getApprovedBy());
        e.setRejectedBy(l.getRejectedBy());
        e.setRejectionReason(l.getRejectionReason());
        return e;
    }

    private Leave toDomain(LeaveJpaEntity e) {
        Leave l = Leave.builder()
            .id(e.getId())
            .tenantId(e.getTenantId())
            .employeeId(e.getEmployeeId())
            .leaveType(Leave.LeaveType.valueOf(e.getLeaveType()))
            .startDate(e.getStartDate())
            .endDate(e.getEndDate())
            .reason(e.getReason())
            .build();
        return l;
    }
}
