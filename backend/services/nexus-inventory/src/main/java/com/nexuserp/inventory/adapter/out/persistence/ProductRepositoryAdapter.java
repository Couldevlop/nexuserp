package com.nexuserp.inventory.adapter.out.persistence;

import com.nexuserp.core.domain.value.Money;
import com.nexuserp.inventory.domain.model.Product;
import com.nexuserp.inventory.domain.port.out.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    public ProductRepositoryAdapter(ProductJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = toJpaEntity(product);
        ProductJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Product> findById(UUID id, String tenantId) {
        return jpaRepository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public Optional<Product> findByCode(String code, String tenantId) {
        return jpaRepository.findByProductCodeAndTenantId(code, tenantId).map(this::toDomain);
    }

    @Override
    public Page<Product> findAll(String tenantId, String category, Pageable pageable) {
        if (category != null && !category.isBlank()) {
            return jpaRepository.findByTenantIdAndCategory(tenantId, category, pageable).map(this::toDomain);
        }
        return jpaRepository.findByTenantId(tenantId, pageable).map(this::toDomain);
    }

    @Override
    public List<Product> findBelowReorderPoint(String tenantId) {
        return jpaRepository.findBelowReorderPoint(tenantId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    private ProductJpaEntity toJpaEntity(Product p) {
        ProductJpaEntity e = new ProductJpaEntity();
        e.setId(p.getId());
        e.setTenantId(p.getTenantId().value());
        e.setProductCode(p.getProductCode());
        e.setName(p.getName());
        e.setDescription(p.getDescription());
        e.setCategory(p.getCategory());
        e.setUnit(p.getUnit());
        e.setStatus(p.getStatus().name());
        e.setQuantityOnHand(p.getQuantityOnHand());
        e.setQuantityReserved(p.getQuantityReserved());
        e.setReorderPoint(p.getReorderPoint());
        e.setReorderQuantity(p.getReorderQuantity());
        e.setSafetyStock(p.getSafetyStock());
        e.setValuationMethod(p.getValuationMethod().name());
        if (p.getStandardCost() != null) {
            e.setStandardCostAmount(p.getStandardCost().amount());
            e.setStandardCostCurrency(p.getStandardCost().currency());
        }
        if (p.getAverageCost() != null) {
            e.setAverageCostAmount(p.getAverageCost().amount());
            e.setAverageCostCurrency(p.getAverageCost().currency());
        }
        e.setWarehouseId(p.getWarehouseId());
        e.setWarehouseLocation(p.getWarehouseLocation());
        e.setSerialTracked(p.isSerialTracked());
        e.setLotTracked(p.isLotTracked());
        e.setExpiryTracked(p.isExpiryTracked());
        return e;
    }

    private Product toDomain(ProductJpaEntity e) {
        Money standardCost = e.getStandardCostAmount() != null
            ? Money.of(e.getStandardCostAmount(), e.getStandardCostCurrency()) : null;
        Money averageCost = e.getAverageCostAmount() != null
            ? Money.of(e.getAverageCostAmount(), e.getAverageCostCurrency()) : null;

        return Product.builder()
            .id(e.getId())
            .tenantId(e.getTenantId())
            .productCode(e.getProductCode())
            .name(e.getName())
            .description(e.getDescription())
            .category(e.getCategory())
            .unit(e.getUnit())
            .quantityOnHand(e.getQuantityOnHand() != null ? e.getQuantityOnHand() : BigDecimal.ZERO)
            .reorderPoint(e.getReorderPoint())
            .reorderQuantity(e.getReorderQuantity())
            .safetyStock(e.getSafetyStock())
            .valuationMethod(Product.StockValuationMethod.valueOf(e.getValuationMethod()))
            .standardCost(standardCost)
            .averageCost(averageCost)
            .warehouseId(e.getWarehouseId())
            .warehouseLocation(e.getWarehouseLocation())
            .serialTracked(e.isSerialTracked())
            .lotTracked(e.isLotTracked())
            .expiryTracked(e.isExpiryTracked())
            .build();
    }
}
