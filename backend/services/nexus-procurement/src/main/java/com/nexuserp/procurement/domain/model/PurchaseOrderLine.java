package com.nexuserp.procurement.domain.model;

import com.nexuserp.core.domain.value.Money;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Getter
public class PurchaseOrderLine {

    private final String id;
    private final String productId;
    private final String description;
    private BigDecimal quantityOrdered;
    private BigDecimal quantityReceived;
    private final BigDecimal unitPrice;
    private final BigDecimal discountPercent;
    private final BigDecimal taxRate;
    private final String currency;

    private Money subtotal;
    private Money taxAmount;
    private Money total;

    /**
     * Factory for use from command data (generates UUID if id is null).
     */
    public static PurchaseOrderLine of(int lineNumber, String productCode, String description,
                                        BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate) {
        return PurchaseOrderLine.builder()
            .id(UUID.randomUUID().toString())
            .productId(productCode)
            .description(description)
            .quantityOrdered(quantity)
            .unitPrice(unitPrice)
            .taxRate(taxRate != null ? taxRate : new BigDecimal("20.00"))
            .currency("EUR")
            .build();
    }

    /**
     * Immutable data transfer for command creation.
     */
    public record LineData(String productCode, String description,
                            BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate) {}

    @Builder
    private PurchaseOrderLine(String id, String productId, String description,
                               BigDecimal quantityOrdered, BigDecimal unitPrice,
                               BigDecimal discountPercent, BigDecimal taxRate, String currency) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.productId = productId;
        this.description = Objects.requireNonNull(description);
        this.quantityOrdered = Objects.requireNonNull(quantityOrdered);
        this.quantityReceived = BigDecimal.ZERO;
        this.unitPrice = Objects.requireNonNull(unitPrice);
        this.discountPercent = discountPercent != null ? discountPercent : BigDecimal.ZERO;
        this.taxRate = taxRate != null ? taxRate : BigDecimal.ZERO;
        this.currency = Objects.requireNonNull(currency);
        recalculate();
    }

    void recalculate() {
        BigDecimal base = quantityOrdered
                .multiply(unitPrice)
                .multiply(BigDecimal.ONE.subtract(discountPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal tax = base.multiply(taxRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                .setScale(4, RoundingMode.HALF_UP);

        this.subtotal = Money.of(base, currency);
        this.taxAmount = Money.of(tax, currency);
        this.total = Money.of(base.add(tax), currency);
    }

    public void recordReceipt(BigDecimal qty) {
        this.quantityReceived = this.quantityReceived.add(qty);
    }

    public boolean isFullyReceived() {
        return quantityReceived.compareTo(quantityOrdered) >= 0;
    }

    public BigDecimal getPendingQuantity() {
        return quantityOrdered.subtract(quantityReceived).max(BigDecimal.ZERO);
    }
}
