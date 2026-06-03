package com.nexuserp.inventory.domain.model;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.Money;
import com.nexuserp.core.domain.value.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Agrégat Produit/Article — Module Stock.
 * Gère : niveaux de stock, alertes, mouvements, valorisation.
 */
public class Product {

    public enum StockValuationMethod { STANDARD, FIFO, LIFO, PMP_PERIOD, PMP_REALTIME }
    public enum ProductStatus { ACTIVE, INACTIVE, DISCONTINUED }

    private final UUID id;
    private final TenantId tenantId;
    private final String productCode;
    private String name;
    private String description;
    private String category;
    private String unit;
    private ProductStatus status;

    // Stock
    private BigDecimal quantityOnHand;
    private BigDecimal quantityReserved;
    private BigDecimal reorderPoint;       // Seuil de réapprovisionnement
    private BigDecimal reorderQuantity;    // Quantité à commander
    private BigDecimal safetyStock;        // Stock de sécurité

    // Valorisation
    private StockValuationMethod valuationMethod;
    private Money standardCost;
    private Money averageCost;
    private String warehouseId;

    // Traçabilité
    private boolean serialTracked;
    private boolean lotTracked;
    private boolean expiryTracked;

    // Emplacement
    private String warehouseLocation;

    private Product(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.tenantId = builder.tenantId;
        this.productCode = builder.productCode;
        this.name = builder.name;
        this.description = builder.description;
        this.category = builder.category;
        this.unit = builder.unit;
        this.status = ProductStatus.ACTIVE;
        this.quantityOnHand = builder.quantityOnHand != null ? builder.quantityOnHand : BigDecimal.ZERO;
        this.quantityReserved = BigDecimal.ZERO;
        this.reorderPoint = builder.reorderPoint != null ? builder.reorderPoint : BigDecimal.ZERO;
        this.reorderQuantity = builder.reorderQuantity != null ? builder.reorderQuantity : BigDecimal.ZERO;
        this.safetyStock = builder.safetyStock != null ? builder.safetyStock : BigDecimal.ZERO;
        this.valuationMethod = builder.valuationMethod != null ? builder.valuationMethod : StockValuationMethod.PMP_REALTIME;
        this.standardCost = builder.standardCost;
        this.averageCost = builder.averageCost;
        this.warehouseId = builder.warehouseId;
        this.serialTracked = builder.serialTracked;
        this.lotTracked = builder.lotTracked;
        this.expiryTracked = builder.expiryTracked;
        this.warehouseLocation = builder.warehouseLocation;
    }

    // ─── Méthodes domaine ───────────────────────────────────────────────────

    /**
     * Entrée en stock (réception, production).
     */
    public void receiveStock(BigDecimal quantity, Money unitCost) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw DomainException.of("INVALID_QUANTITY", "Received quantity must be positive");
        }
        this.quantityOnHand = this.quantityOnHand.add(quantity);
        updateAverageCost(quantity, unitCost);
    }

    /**
     * Sortie de stock (vente, consommation production).
     */
    public void issueStock(BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw DomainException.of("INVALID_QUANTITY", "Issue quantity must be positive");
        }
        BigDecimal available = quantityOnHand.subtract(quantityReserved);
        if (quantity.compareTo(available) > 0) {
            throw DomainException.of("INSUFFICIENT_STOCK",
                "Insufficient stock for product " + productCode +
                ": available=" + available + ", requested=" + quantity);
        }
        this.quantityOnHand = this.quantityOnHand.subtract(quantity);
    }

    /**
     * Réservation stock pour commande client.
     */
    public void reserveStock(BigDecimal quantity) {
        BigDecimal available = quantityOnHand.subtract(quantityReserved);
        if (quantity.compareTo(available) > 0) {
            throw DomainException.of("INSUFFICIENT_STOCK",
                "Cannot reserve " + quantity + " units — only " + available + " available");
        }
        this.quantityReserved = this.quantityReserved.add(quantity);
    }

    public void releaseReservation(BigDecimal quantity) {
        this.quantityReserved = this.quantityReserved.subtract(quantity)
            .max(BigDecimal.ZERO);
    }

    /**
     * Vérifie si le stock est en-dessous du seuil de réapprovisionnement.
     */
    public boolean needsReorder() {
        return quantityOnHand.compareTo(reorderPoint) <= 0;
    }

    public boolean isBelowSafetyStock() {
        return quantityOnHand.compareTo(safetyStock) < 0;
    }

    public BigDecimal getAvailableQuantity() {
        return quantityOnHand.subtract(quantityReserved);
    }

    /**
     * Mise à jour du coût moyen pondéré (CMP).
     */
    private void updateAverageCost(BigDecimal newQuantity, Money newCost) {
        if (valuationMethod != StockValuationMethod.PMP_REALTIME) return;
        if (newCost == null || averageCost == null) {
            this.averageCost = newCost;
            return;
        }
        BigDecimal totalExistingValue = averageCost.amount().multiply(quantityOnHand.subtract(newQuantity));
        BigDecimal totalNewValue = newCost.amount().multiply(newQuantity);
        BigDecimal totalQty = quantityOnHand;
        if (totalQty.compareTo(BigDecimal.ZERO) > 0) {
            this.averageCost = Money.of(
                totalExistingValue.add(totalNewValue).divide(totalQty, 4, java.math.RoundingMode.HALF_UP),
                averageCost.currency()
            );
        }
    }

    // Getters
    public UUID getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public String getProductCode() { return productCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getUnit() { return unit; }
    public ProductStatus getStatus() { return status; }
    public BigDecimal getQuantityOnHand() { return quantityOnHand; }
    public BigDecimal getQuantityReserved() { return quantityReserved; }
    public BigDecimal getReorderPoint() { return reorderPoint; }
    public BigDecimal getReorderQuantity() { return reorderQuantity; }
    public BigDecimal getSafetyStock() { return safetyStock; }
    public StockValuationMethod getValuationMethod() { return valuationMethod; }
    public Money getStandardCost() { return standardCost; }
    public Money getAverageCost() { return averageCost; }
    public String getWarehouseId() { return warehouseId; }
    public boolean isSerialTracked() { return serialTracked; }
    public boolean isLotTracked() { return lotTracked; }
    public boolean isExpiryTracked() { return expiryTracked; }
    public String getWarehouseLocation() { return warehouseLocation; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private TenantId tenantId;
        private String productCode;
        private String name;
        private String description;
        private String category;
        private String unit = "UNIT";
        private BigDecimal quantityOnHand;
        private BigDecimal reorderPoint;
        private BigDecimal reorderQuantity;
        private BigDecimal safetyStock;
        private StockValuationMethod valuationMethod;
        private Money standardCost;
        private Money averageCost;
        private String warehouseId;
        private boolean serialTracked;
        private boolean lotTracked;
        private boolean expiryTracked;
        private String warehouseLocation;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(String t) { this.tenantId = TenantId.of(t); return this; }
        public Builder productCode(String c) { this.productCode = c; return this; }
        public Builder name(String n) { this.name = n; return this; }
        public Builder description(String d) { this.description = d; return this; }
        public Builder category(String c) { this.category = c; return this; }
        public Builder unit(String u) { this.unit = u; return this; }
        public Builder quantityOnHand(BigDecimal q) { this.quantityOnHand = q; return this; }
        public Builder reorderPoint(BigDecimal r) { this.reorderPoint = r; return this; }
        public Builder reorderQuantity(BigDecimal r) { this.reorderQuantity = r; return this; }
        public Builder safetyStock(BigDecimal s) { this.safetyStock = s; return this; }
        public Builder valuationMethod(StockValuationMethod m) { this.valuationMethod = m; return this; }
        public Builder standardCost(Money m) { this.standardCost = m; return this; }
        public Builder averageCost(Money m) { this.averageCost = m; return this; }
        public Builder warehouseId(String w) { this.warehouseId = w; return this; }
        public Builder serialTracked(boolean b) { this.serialTracked = b; return this; }
        public Builder lotTracked(boolean b) { this.lotTracked = b; return this; }
        public Builder expiryTracked(boolean b) { this.expiryTracked = b; return this; }
        public Builder warehouseLocation(String l) { this.warehouseLocation = l; return this; }

        public Product build() {
            if (tenantId == null) throw new IllegalStateException("tenantId required");
            if (productCode == null || productCode.isBlank()) throw new IllegalStateException("productCode required");
            if (name == null || name.isBlank()) throw new IllegalStateException("name required");
            return new Product(this);
        }
    }
}
