package com.nexuserp.hr.adapter.in.rest;

import com.nexuserp.hr.domain.model.Leave;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveDto(
    UUID id,
    UUID employeeId,
    String leaveType,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    long durationDays,
    String reason,
    String approvedBy,
    String rejectedBy,
    String rejectionReason
) {
    public static LeaveDto from(Leave l) {
        return new LeaveDto(
            l.getId(), l.getEmployeeId(),
            l.getLeaveType().name(), l.getStatus().name(),
            l.getStartDate(), l.getEndDate(), l.getDurationDays(),
            l.getReason(), l.getApprovedBy(), l.getRejectedBy(), l.getRejectionReason()
        );
    }
}
