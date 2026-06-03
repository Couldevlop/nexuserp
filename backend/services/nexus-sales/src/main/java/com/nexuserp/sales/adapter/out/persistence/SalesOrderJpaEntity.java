package com.nexuserp.sales.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sales_orders", schema = "nexus_sales")
public class SalesOrderJpaEntity {

    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
    @Column(name = "order_number", nullable = false, length = 30) private String orderNumber;
    @Column(name = "customer_id") private UUID customerId;
    @Column(name = "customer_name") private String customerName;
    @Column(name = "customer_ref", length = 50) private String customerRef;
    @Column(name = "status", length = 20) private String status;
    @Column(name = "order_date") private LocalDate orderDate;
    @Column(name = "requested_delivery_date") private LocalDate requestedDeliveryDate;
    @Column(name = "actual_delivery_date") private LocalDate actualDeliveryDate;
    @Column(name = "currency", length = 3) private String currency;
    @Column(name = "subtotal_amount", precision = 19, scale = 4) private BigDecimal subtotalAmount;
    @Column(name = "tax_amount", precision = 19, scale = 4) private BigDecimal taxAmount;
    @Column(name = "total_amount", precision = 19, scale = 4) private BigDecimal totalAmount;
    @Column(name = "shipping_address", columnDefinition = "TEXT") private String shippingAddress;
    @Column(name = "notes", columnDefinition = "TEXT") private String notes;
    @Column(name = "created_by") private String createdBy;
    @Column(name = "created_at", updatable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = Instant.now(); }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public String getTenantId() { return tenantId; } public void setTenantId(String t) { this.tenantId = t; }
    public String getOrderNumber() { return orderNumber; } public void setOrderNumber(String n) { this.orderNumber = n; }
    public UUID getCustomerId() { return customerId; } public void setCustomerId(UUID c) { this.customerId = c; }
    public String getCustomerName() { return customerName; } public void setCustomerName(String c) { this.customerName = c; }
    public String getCustomerRef() { return customerRef; } public void setCustomerRef(String r) { this.customerRef = r; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public LocalDate getOrderDate() { return orderDate; } public void setOrderDate(LocalDate d) { this.orderDate = d; }
    public LocalDate getRequestedDeliveryDate() { return requestedDeliveryDate; } public void setRequestedDeliveryDate(LocalDate d) { this.requestedDeliveryDate = d; }
    public LocalDate getActualDeliveryDate() { return actualDeliveryDate; } public void setActualDeliveryDate(LocalDate d) { this.actualDeliveryDate = d; }
    public String getCurrency() { return currency; } public void setCurrency(String c) { this.currency = c; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; } public void setSubtotalAmount(BigDecimal a) { this.subtotalAmount = a; }
    public BigDecimal getTaxAmount() { return taxAmount; } public void setTaxAmount(BigDecimal a) { this.taxAmount = a; }
    public BigDecimal getTotalAmount() { return totalAmount; } public void setTotalAmount(BigDecimal a) { this.totalAmount = a; }
    public String getShippingAddress() { return shippingAddress; } public void setShippingAddress(String a) { this.shippingAddress = a; }
    public String getNotes() { return notes; } public void setNotes(String n) { this.notes = n; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String c) { this.createdBy = c; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
