package com.nexuserp.sales.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class SalesOrderLine {

    private final UUID id;
    private final int lineNumber;
    private UUID productId;
    private String productCode;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPct;
    private BigDecimal taxRate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private BigDecimal quantityDelivered;
    private BigDecimal quantityInvoiced;

    private SalesOrderLine(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.lineNumber = builder.lineNumber;
        this.productId = builder.productId;
        this.productCode = builder.productCode;
        this.description = builder.description;
        this.quantity = builder.quantity;
        this.unitPrice = builder.unitPrice;
        this.discountPct = builder.discountPct != null ? builder.discountPct : BigDecimal.ZERO;
        this.taxRate = builder.taxRate != null ? builder.taxRate : new BigDecimal("20.00");
        this.quantityDelivered = BigDecimal.ZERO;
        this.quantityInvoiced = BigDecimal.ZERO;
        calculate();
    }

    private void calculate() {
        BigDecimal discount = discountPct.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        this.subtotal = quantity.multiply(unitPrice)
            .multiply(BigDecimal.ONE.subtract(discount))
            .setScale(4, RoundingMode.HALF_UP);
        this.taxAmount = subtotal.multiply(taxRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
            .setScale(4, RoundingMode.HALF_UP);
        this.total = subtotal.add(taxAmount).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal getPendingDeliveryQuantity() {
        return quantity.subtract(quantityDelivered);
    }

    public UUID getId() { return id; }
    public int getLineNumber() { return lineNumber; }
    public UUID getProductId() { return productId; }
    public String getProductCode() { return productCode; }
    public String getDescription() { return description; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getDiscountPct() { return discountPct; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getQuantityDelivered() { return quantityDelivered; }
    public BigDecimal getQuantityInvoiced() { return quantityInvoiced; }

    /**
     * Factory method for creating from command data.
     */
    public static SalesOrderLine of(int lineNumber, String productCode, String productName,
                                     java.math.BigDecimal quantity, java.math.BigDecimal unitPrice,
                                     java.math.BigDecimal taxRate) {
        return SalesOrderLine.builder()
            .lineNumber(lineNumber)
            .productCode(productCode)
            .description(productName != null ? productName : productCode)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .taxRate(taxRate != null ? taxRate : new java.math.BigDecimal("20.00"))
            .build();
    }

    /**
     * Immutable data for command transfer.
     */
    public record LineData(String productCode, String productName,
                            java.math.BigDecimal quantity, java.math.BigDecimal unitPrice,
                            java.math.BigDecimal taxRate) {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private int lineNumber;
        private UUID productId;
        private String productCode;
        private String description;
        private BigDecimal quantity = BigDecimal.ONE;
        private BigDecimal unitPrice = BigDecimal.ZERO;
        private BigDecimal discountPct;
        private BigDecimal taxRate;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder lineNumber(int n) { this.lineNumber = n; return this; }
        public Builder productId(UUID id) { this.productId = id; return this; }
        public Builder productCode(String c) { this.productCode = c; return this; }
        public Builder description(String d) { this.description = d; return this; }
        public Builder quantity(BigDecimal q) { this.quantity = q; return this; }
        public Builder unitPrice(BigDecimal p) { this.unitPrice = p; return this; }
        public Builder discountPct(BigDecimal d) { this.discountPct = d; return this; }
        public Builder taxRate(BigDecimal t) { this.taxRate = t; return this; }

        public SalesOrderLine build() {
            if (description == null || description.isBlank()) throw new IllegalStateException("description required");
            return new SalesOrderLine(this);
        }
    }
}
