package com.nexuserp.procurement.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "purchase_orders", schema = "nexus_procurement")
public class PurchaseOrderJpaEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "tenant_id", nullable = false) private String tenantId;
    @Column(name = "po_number", nullable = false, length = 30) private String poNumber;
    @Column(name = "supplier_id", length = 36) private String supplierId;
    @Column(name = "supplier_name") private String supplierName;
    @Column(name = "status", length = 20) private String status;
    @Column(name = "expected_delivery_date") private LocalDate expectedDeliveryDate;
    @Column(name = "actual_delivery_date") private LocalDate actualDeliveryDate;
    @Column(name = "currency", length = 3) private String currency;
    @Column(name = "subtotal_amount", precision = 19, scale = 4) private BigDecimal subtotalAmount;
    @Column(name = "tax_amount", precision = 19, scale = 4) private BigDecimal taxAmount;
    @Column(name = "total_amount", precision = 19, scale = 4) private BigDecimal totalAmount;
    @Column(name = "notes", columnDefinition = "TEXT") private String notes;
    @Column(name = "approved_by") private String approvedBy;
    @Column(name = "created_by") private String createdBy;
    @Column(name = "created_at", updatable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = Instant.now(); }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; } public void setTenantId(String t) { this.tenantId = t; }
    public String getPoNumber() { return poNumber; } public void setPoNumber(String n) { this.poNumber = n; }
    public String getSupplierId() { return supplierId; } public void setSupplierId(String s) { this.supplierId = s; }
    public String getSupplierName() { return supplierName; } public void setSupplierName(String s) { this.supplierName = s; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; } public void setExpectedDeliveryDate(LocalDate d) { this.expectedDeliveryDate = d; }
    public LocalDate getActualDeliveryDate() { return actualDeliveryDate; } public void setActualDeliveryDate(LocalDate d) { this.actualDeliveryDate = d; }
    public String getCurrency() { return currency; } public void setCurrency(String c) { this.currency = c; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; } public void setSubtotalAmount(BigDecimal a) { this.subtotalAmount = a; }
    public BigDecimal getTaxAmount() { return taxAmount; } public void setTaxAmount(BigDecimal a) { this.taxAmount = a; }
    public BigDecimal getTotalAmount() { return totalAmount; } public void setTotalAmount(BigDecimal a) { this.totalAmount = a; }
    public String getNotes() { return notes; } public void setNotes(String n) { this.notes = n; }
    public String getApprovedBy() { return approvedBy; } public void setApprovedBy(String a) { this.approvedBy = a; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String c) { this.createdBy = c; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
