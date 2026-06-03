package com.nexuserp.inventory.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockLowAlertEvent(
    String eventId,
    String tenantId,
    UUID productId,
    String productCode,
    String productName,
    BigDecimal currentQuantity,
    BigDecimal reorderPoint,
    Instant occurredAt
) {
    public StockLowAlertEvent(String tenantId, UUID productId, String productCode,
                               String productName, BigDecimal currentQty, BigDecimal reorderPoint) {
        this(UUID.randomUUID().toString(), tenantId, productId, productCode, productName,
             currentQty, reorderPoint, Instant.now());
    }
}
