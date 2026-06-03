package com.nexuserp.inventory.domain.port.in;

import com.nexuserp.core.domain.value.Money;
import com.nexuserp.inventory.domain.model.Product;

import java.math.BigDecimal;
import java.util.UUID;

public interface StockMovementUseCase {
    Product receiveStock(UUID productId, String tenantId, BigDecimal quantity, Money unitCost, String reference);
    Product issueStock(UUID productId, String tenantId, BigDecimal quantity, String reference);
    Product adjustStock(UUID productId, String tenantId, BigDecimal newQuantity, String reason);
}
