package com.nexuserp.inventory.application.command;

import com.nexuserp.inventory.domain.model.Product;

import java.math.BigDecimal;

public record CreateProductCommand(
    String tenantId,
    String productCode,
    String name,
    String description,
    String category,
    String unit,
    BigDecimal reorderPoint,
    BigDecimal reorderQuantity,
    BigDecimal safetyStock,
    Product.StockValuationMethod valuationMethod,
    String warehouseId,
    String warehouseLocation,
    boolean serialTracked,
    boolean lotTracked,
    boolean expiryTracked,
    String createdBy
) {}
