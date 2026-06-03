package com.nexuserp.hr.domain.model;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.TenantId;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Agrégat Demande de Congé — Domaine RH.
 */
public class Leave {

    public enum LeaveType { ANNUAL, SICK, MATERNITY, PATERNITY, RTT, UNPAID, OTHER }
    public enum LeaveStatus { DRAFT, SUBMITTED, APPROVED, REJECTED, CANCELLED }

    private final UUID id;
    private final TenantId tenantId;
    private final UUID employeeId;
    private final LeaveType leaveType;
    private LeaveStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private long durationDays;
    private String reason;
    private String approvedBy;
    private String rejectedBy;
    private String rejectionReason;

    private Leave(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.tenantId = TenantId.of(builder.tenantId);
        this.employeeId = builder.employeeId;
        this.leaveType = builder.leaveType;
        this.status = LeaveStatus.DRAFT;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.durationDays = ChronoUnit.DAYS.between(builder.startDate, builder.endDate) + 1;
        this.reason = builder.reason;

        if (builder.startDate.isAfter(builder.endDate)) {
            throw DomainException.of("INVALID_LEAVE_DATES", "Start date must be before end date");
        }
    }

    public void submit() {
        if (status != LeaveStatus.DRAFT) {
            throw DomainException.invalidState("Leave", status.name(), "DRAFT");
        }
        this.status = LeaveStatus.SUBMITTED;
    }

    public void approve(String approvedBy) {
        if (status != LeaveStatus.SUBMITTED) {
            throw DomainException.invalidState("Leave", status.name(), "SUBMITTED");
        }
        this.status = LeaveStatus.APPROVED;
        this.approvedBy = approvedBy;
    }

    public void reject(String rejectedBy, String reason) {
        if (status != LeaveStatus.SUBMITTED) {
            throw DomainException.invalidState("Leave", status.name(), "SUBMITTED");
        }
        this.status = LeaveStatus.REJECTED;
        this.rejectedBy = rejectedBy;
        this.rejectionReason = reason;
    }

    public void cancel() {
        if (status == LeaveStatus.APPROVED || status == LeaveStatus.REJECTED) {
            throw DomainException.invalidState("Leave", status.name(), "DRAFT or SUBMITTED");
        }
        this.status = LeaveStatus.CANCELLED;
    }

    // Getters
    public UUID getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public UUID getEmployeeId() { return employeeId; }
    public LeaveType getLeaveType() { return leaveType; }
    public LeaveStatus getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public long getDurationDays() { return durationDays; }
    public String getReason() { return reason; }
    public String getApprovedBy() { return approvedBy; }
    public String getRejectedBy() { return rejectedBy; }
    public String getRejectionReason() { return rejectionReason; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String tenantId;
        private UUID employeeId;
        private LeaveType leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(String t) { this.tenantId = t; return this; }
        public Builder employeeId(UUID e) { this.employeeId = e; return this; }
        public Builder leaveType(LeaveType t) { this.leaveType = t; return this; }
        public Builder startDate(LocalDate d) { this.startDate = d; return this; }
        public Builder endDate(LocalDate d) { this.endDate = d; return this; }
        public Builder reason(String r) { this.reason = r; return this; }

        public Leave build() {
            if (tenantId == null) throw new IllegalStateException("tenantId required");
            if (employeeId == null) throw new IllegalStateException("employeeId required");
            if (leaveType == null) throw new IllegalStateException("leaveType required");
            if (startDate == null || endDate == null) throw new IllegalStateException("dates required");
            return new Leave(this);
        }
    }
}
