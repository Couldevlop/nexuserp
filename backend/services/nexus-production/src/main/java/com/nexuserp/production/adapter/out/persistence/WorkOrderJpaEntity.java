package com.nexuserp.production.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_orders", schema = "nexus_production")
public class WorkOrderJpaEntity {

    @Id @Column(name = "id", length = 36) private String id;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
    @Column(name = "order_number", nullable = false, length = 30) private String orderNumber;
    @Column(name = "product_id", length = 36) private String productId;
    @Column(name = "product_name") private String productName;
    @Column(name = "bom_id", length = 36) private String bomId;
    @Column(name = "routing_id", length = 36) private String routingId;
    @Column(name = "status", length = 20) private String status;
    @Column(name = "priority", length = 10) private String priority;
    @Column(name = "quantity_planned", precision = 19, scale = 4) private BigDecimal quantityPlanned;
    @Column(name = "quantity_produced", precision = 19, scale = 4) private BigDecimal quantityProduced;
    @Column(name = "quantity_rejected", precision = 19, scale = 4) private BigDecimal quantityRejected;
    @Column(name = "planned_start_date") private LocalDate plannedStartDate;
    @Column(name = "planned_end_date") private LocalDate plannedEndDate;
    @Column(name = "actual_start_date") private LocalDateTime actualStartDate;
    @Column(name = "actual_end_date") private LocalDateTime actualEndDate;
    @Column(name = "workcenter") private String workcenter;
    @Column(name = "operator") private String operator;
    @Column(name = "notes", columnDefinition = "TEXT") private String notes;
    @Column(name = "created_at", updatable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = Instant.now(); }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; } public void setTenantId(String t) { this.tenantId = t; }
    public String getOrderNumber() { return orderNumber; } public void setOrderNumber(String n) { this.orderNumber = n; }
    public String getProductId() { return productId; } public void setProductId(String p) { this.productId = p; }
    public String getProductName() { return productName; } public void setProductName(String p) { this.productName = p; }
    public String getBomId() { return bomId; } public void setBomId(String b) { this.bomId = b; }
    public String getRoutingId() { return routingId; } public void setRoutingId(String r) { this.routingId = r; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public String getPriority() { return priority; } public void setPriority(String p) { this.priority = p; }
    public BigDecimal getQuantityPlanned() { return quantityPlanned; } public void setQuantityPlanned(BigDecimal q) { this.quantityPlanned = q; }
    public BigDecimal getQuantityProduced() { return quantityProduced; } public void setQuantityProduced(BigDecimal q) { this.quantityProduced = q; }
    public BigDecimal getQuantityRejected() { return quantityRejected; } public void setQuantityRejected(BigDecimal q) { this.quantityRejected = q; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; } public void setPlannedStartDate(LocalDate d) { this.plannedStartDate = d; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; } public void setPlannedEndDate(LocalDate d) { this.plannedEndDate = d; }
    public LocalDateTime getActualStartDate() { return actualStartDate; } public void setActualStartDate(LocalDateTime d) { this.actualStartDate = d; }
    public LocalDateTime getActualEndDate() { return actualEndDate; } public void setActualEndDate(LocalDateTime d) { this.actualEndDate = d; }
    public String getWorkcenter() { return workcenter; } public void setWorkcenter(String w) { this.workcenter = w; }
    public String getOperator() { return operator; } public void setOperator(String o) { this.operator = o; }
    public String getNotes() { return notes; } public void setNotes(String n) { this.notes = n; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
