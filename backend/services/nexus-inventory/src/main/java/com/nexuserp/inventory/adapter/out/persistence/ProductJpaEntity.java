package com.nexuserp.inventory.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "nexus_inventory",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "product_code"}))
public class ProductJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "quantity_on_hand", precision = 19, scale = 4)
    private BigDecimal quantityOnHand;

    @Column(name = "quantity_reserved", precision = 19, scale = 4)
    private BigDecimal quantityReserved;

    @Column(name = "reorder_point", precision = 19, scale = 4)
    private BigDecimal reorderPoint;

    @Column(name = "reorder_quantity", precision = 19, scale = 4)
    private BigDecimal reorderQuantity;

    @Column(name = "safety_stock", precision = 19, scale = 4)
    private BigDecimal safetyStock;

    @Column(name = "valuation_method", length = 20)
    private String valuationMethod;

    @Column(name = "standard_cost_amount", precision = 19, scale = 4)
    private BigDecimal standardCostAmount;

    @Column(name = "standard_cost_currency", length = 3)
    private String standardCostCurrency;

    @Column(name = "average_cost_amount", precision = 19, scale = 4)
    private BigDecimal averageCostAmount;

    @Column(name = "average_cost_currency", length = 3)
    private String averageCostCurrency;

    @Column(name = "warehouse_id")
    private String warehouseId;

    @Column(name = "warehouse_location")
    private String warehouseLocation;

    @Column(name = "serial_tracked")
    private boolean serialTracked;

    @Column(name = "lot_tracked")
    private boolean lotTracked;

    @Column(name = "expiry_tracked")
    private boolean expiryTracked;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(BigDecimal quantityOnHand) { this.quantityOnHand = quantityOnHand; }
    public BigDecimal getQuantityReserved() { return quantityReserved; }
    public void setQuantityReserved(BigDecimal quantityReserved) { this.quantityReserved = quantityReserved; }
    public BigDecimal getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(BigDecimal reorderPoint) { this.reorderPoint = reorderPoint; }
    public BigDecimal getReorderQuantity() { return reorderQuantity; }
    public void setReorderQuantity(BigDecimal reorderQuantity) { this.reorderQuantity = reorderQuantity; }
    public BigDecimal getSafetyStock() { return safetyStock; }
    public void setSafetyStock(BigDecimal safetyStock) { this.safetyStock = safetyStock; }
    public String getValuationMethod() { return valuationMethod; }
    public void setValuationMethod(String valuationMethod) { this.valuationMethod = valuationMethod; }
    public BigDecimal getStandardCostAmount() { return standardCostAmount; }
    public void setStandardCostAmount(BigDecimal standardCostAmount) { this.standardCostAmount = standardCostAmount; }
    public String getStandardCostCurrency() { return standardCostCurrency; }
    public void setStandardCostCurrency(String standardCostCurrency) { this.standardCostCurrency = standardCostCurrency; }
    public BigDecimal getAverageCostAmount() { return averageCostAmount; }
    public void setAverageCostAmount(BigDecimal averageCostAmount) { this.averageCostAmount = averageCostAmount; }
    public String getAverageCostCurrency() { return averageCostCurrency; }
    public void setAverageCostCurrency(String averageCostCurrency) { this.averageCostCurrency = averageCostCurrency; }
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseLocation() { return warehouseLocation; }
    public void setWarehouseLocation(String warehouseLocation) { this.warehouseLocation = warehouseLocation; }
    public boolean isSerialTracked() { return serialTracked; }
    public void setSerialTracked(boolean serialTracked) { this.serialTracked = serialTracked; }
    public boolean isLotTracked() { return lotTracked; }
    public void setLotTracked(boolean lotTracked) { this.lotTracked = lotTracked; }
    public boolean isExpiryTracked() { return expiryTracked; }
    public void setExpiryTracked(boolean expiryTracked) { this.expiryTracked = expiryTracked; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
