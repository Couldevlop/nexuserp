package com.nexuserp.finance.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Ligne de facture.
 * Calcule automatiquement les montants depuis quantité/prix/taux.
 */
public class InvoiceLine {

    private final UUID id;
    private final int lineNumber;
    private String description;
    private String productCode;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPct;    // 0-100
    private BigDecimal taxRate;        // ex: 20.0 pour 20% TVA
    private BigDecimal subtotal;       // quantité × prix × (1 - remise%)
    private BigDecimal taxAmount;      // subtotal × (taxRate / 100)
    private BigDecimal total;          // subtotal + taxAmount
    private UUID accountId;
    private UUID costCenterId;

    private InvoiceLine(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.lineNumber = builder.lineNumber;
        this.description = builder.description;
        this.productCode = builder.productCode;
        this.quantity = builder.quantity;
        this.unitPrice = builder.unitPrice;
        this.discountPct = builder.discountPct;
        this.taxRate = builder.taxRate;
        this.accountId = builder.accountId;
        this.costCenterId = builder.costCenterId;
        calculate();
    }

    private void calculate() {
        // subtotal = qty × price × (1 - discount/100)
        BigDecimal discount = discountPct.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        this.subtotal = quantity.multiply(unitPrice)
            .multiply(BigDecimal.ONE.subtract(discount))
            .setScale(4, RoundingMode.HALF_UP);

        // taxAmount = subtotal × (taxRate / 100)
        this.taxAmount = subtotal.multiply(taxRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
            .setScale(4, RoundingMode.HALF_UP);

        this.total = subtotal.add(taxAmount).setScale(4, RoundingMode.HALF_UP);
    }

    // Getters
    public UUID getId() { return id; }
    public int getLineNumber() { return lineNumber; }
    public String getDescription() { return description; }
    public String getProductCode() { return productCode; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getDiscountPct() { return discountPct; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotal() { return total; }
    public UUID getAccountId() { return accountId; }
    public UUID getCostCenterId() { return costCenterId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private int lineNumber;
        private String description;
        private String productCode;
        private BigDecimal quantity = BigDecimal.ONE;
        private BigDecimal unitPrice = BigDecimal.ZERO;
        private BigDecimal discountPct = BigDecimal.ZERO;
        private BigDecimal taxRate = new BigDecimal("20.00");  // TVA 20% par défaut France
        private UUID accountId;
        private UUID costCenterId;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder lineNumber(int n) { this.lineNumber = n; return this; }
        public Builder description(String d) { this.description = d; return this; }
        public Builder productCode(String code) { this.productCode = code; return this; }
        public Builder quantity(BigDecimal qty) { this.quantity = qty; return this; }
        public Builder quantity(double qty) { this.quantity = BigDecimal.valueOf(qty); return this; }
        public Builder unitPrice(BigDecimal price) { this.unitPrice = price; return this; }
        public Builder unitPrice(double price) { this.unitPrice = BigDecimal.valueOf(price); return this; }
        public Builder discountPct(BigDecimal pct) { this.discountPct = pct; return this; }
        public Builder taxRate(BigDecimal rate) { this.taxRate = rate; return this; }
        public Builder taxRate(double rate) { this.taxRate = BigDecimal.valueOf(rate); return this; }
        public Builder accountId(UUID id) { this.accountId = id; return this; }
        public Builder costCenterId(UUID id) { this.costCenterId = id; return this; }

        public InvoiceLine build() {
            if (description == null || description.isBlank()) throw new IllegalStateException("description required");
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalStateException("quantity must be positive");
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalStateException("unitPrice cannot be negative");
            return new InvoiceLine(this);
        }
    }
}
