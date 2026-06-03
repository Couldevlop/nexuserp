package com.nexuserp.hr.application.command;

import com.nexuserp.hr.domain.model.Leave;

import java.time.LocalDate;
import java.util.UUID;

public record CreateLeaveCommand(
    String tenantId,
    UUID employeeId,
    Leave.LeaveType leaveType,
    LocalDate startDate,
    LocalDate endDate,
    String reason
) {}
