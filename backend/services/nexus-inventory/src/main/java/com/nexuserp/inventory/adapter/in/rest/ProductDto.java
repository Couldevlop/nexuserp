package com.nexuserp.inventory.adapter.in.rest;

import com.nexuserp.inventory.domain.model.Product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(
    UUID id,
    String tenantId,
    String productCode,
    String name,
    String description,
    String category,
    String unit,
    String status,
    BigDecimal quantityOnHand,
    BigDecimal quantityReserved,
    BigDecimal availableQuantity,
    BigDecimal reorderPoint,
    BigDecimal reorderQuantity,
    BigDecimal safetyStock,
    String valuationMethod,
    BigDecimal averageCostAmount,
    String averageCostCurrency,
    String warehouseId,
    String warehouseLocation,
    boolean serialTracked,
    boolean lotTracked,
    boolean expiryTracked,
    boolean needsReorder
) {
    public static ProductDto from(Product p) {
        return new ProductDto(
            p.getId(),
            p.getTenantId().value(),
            p.getProductCode(),
            p.getName(),
            p.getDescription(),
            p.getCategory(),
            p.getUnit(),
            p.getStatus().name(),
            p.getQuantityOnHand(),
            p.getQuantityReserved(),
            p.getAvailableQuantity(),
            p.getReorderPoint(),
            p.getReorderQuantity(),
            p.getSafetyStock(),
            p.getValuationMethod().name(),
            p.getAverageCost() != null ? p.getAverageCost().amount() : null,
            p.getAverageCost() != null ? p.getAverageCost().currency() : null,
            p.getWarehouseId(),
            p.getWarehouseLocation(),
            p.isSerialTracked(),
            p.isLotTracked(),
            p.isExpiryTracked(),
            p.needsReorder()
        );
    }
}
