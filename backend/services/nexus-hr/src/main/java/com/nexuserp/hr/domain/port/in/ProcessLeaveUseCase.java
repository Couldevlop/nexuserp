package com.nexuserp.hr.domain.port.in;

import com.nexuserp.hr.application.command.CreateLeaveCommand;
import com.nexuserp.hr.domain.model.Leave;

import java.util.UUID;

public interface ProcessLeaveUseCase {
    Leave requestLeave(CreateLeaveCommand command);
    Leave approveLeave(UUID leaveId, String tenantId, String approvedBy);
    Leave rejectLeave(UUID leaveId, String tenantId, String rejectedBy, String reason);
}
