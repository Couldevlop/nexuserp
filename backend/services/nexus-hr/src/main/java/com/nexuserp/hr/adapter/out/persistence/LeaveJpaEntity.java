package com.nexuserp.hr.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leaves", schema = "nexus_hr")
public class LeaveJpaEntity {

    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "leave_type", length = 20) private String leaveType;
    @Column(name = "status", length = 20) private String status;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "duration_days") private long durationDays;
    @Column(name = "reason", columnDefinition = "TEXT") private String reason;
    @Column(name = "approved_by") private String approvedBy;
    @Column(name = "rejected_by") private String rejectedBy;
    @Column(name = "rejection_reason", columnDefinition = "TEXT") private String rejectionReason;
    @Column(name = "created_at", updatable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = Instant.now(); }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public String getTenantId() { return tenantId; } public void setTenantId(String t) { this.tenantId = t; }
    public UUID getEmployeeId() { return employeeId; } public void setEmployeeId(UUID e) { this.employeeId = e; }
    public String getLeaveType() { return leaveType; } public void setLeaveType(String t) { this.leaveType = t; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate d) { this.startDate = d; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate d) { this.endDate = d; }
    public long getDurationDays() { return durationDays; } public void setDurationDays(long d) { this.durationDays = d; }
    public String getReason() { return reason; } public void setReason(String r) { this.reason = r; }
    public String getApprovedBy() { return approvedBy; } public void setApprovedBy(String a) { this.approvedBy = a; }
    public String getRejectedBy() { return rejectedBy; } public void setRejectedBy(String r) { this.rejectedBy = r; }
    public String getRejectionReason() { return rejectionReason; } public void setRejectionReason(String r) { this.rejectionReason = r; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
