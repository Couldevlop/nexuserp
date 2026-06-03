package com.nexuserp.inventory.domain.service;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.Money;
import com.nexuserp.inventory.application.command.CreateProductCommand;
import com.nexuserp.inventory.application.query.ProductPageQuery;
import com.nexuserp.inventory.domain.event.StockLowAlertEvent;
import com.nexuserp.inventory.domain.model.Product;
import com.nexuserp.inventory.domain.port.in.CreateProductUseCase;
import com.nexuserp.inventory.domain.port.in.GetProductUseCase;
import com.nexuserp.inventory.domain.port.in.StockMovementUseCase;
import com.nexuserp.inventory.domain.port.out.InventoryEventPublisher;
import com.nexuserp.inventory.domain.port.out.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class ProductService implements CreateProductUseCase, GetProductUseCase, StockMovementUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final InventoryEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository, InventoryEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Product createProduct(CreateProductCommand cmd) {
        if (productRepository.findByCode(cmd.productCode(), cmd.tenantId()).isPresent()) {
            throw DomainException.of("DUPLICATE_PRODUCT_CODE",
                "Product code already exists: " + cmd.productCode());
        }
        Product product = Product.builder()
            .tenantId(cmd.tenantId())
            .productCode(cmd.productCode())
            .name(cmd.name())
            .description(cmd.description())
            .category(cmd.category())
            .unit(cmd.unit() != null ? cmd.unit() : "UNIT")
            .reorderPoint(cmd.reorderPoint() != null ? cmd.reorderPoint() : BigDecimal.ZERO)
            .reorderQuantity(cmd.reorderQuantity() != null ? cmd.reorderQuantity() : BigDecimal.ZERO)
            .safetyStock(cmd.safetyStock() != null ? cmd.safetyStock() : BigDecimal.ZERO)
            .valuationMethod(cmd.valuationMethod() != null ? cmd.valuationMethod() : Product.StockValuationMethod.PMP_REALTIME)
            .warehouseId(cmd.warehouseId())
            .warehouseLocation(cmd.warehouseLocation())
            .serialTracked(cmd.serialTracked())
            .lotTracked(cmd.lotTracked())
            .expiryTracked(cmd.expiryTracked())
            .build();

        Product saved = productRepository.save(product);
        log.info("Product created: code={}, tenant={}", saved.getProductCode(), cmd.tenantId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Product getById(UUID id, String tenantId) {
        return productRepository.findById(id, tenantId)
            .orElseThrow(() -> DomainException.notFound("Product", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getProducts(ProductPageQuery query) {
        return productRepository.findAll(query.tenantId(), query.category(), query.toPageable());
    }

    @Override
    public Product receiveStock(UUID productId, String tenantId, BigDecimal quantity, Money unitCost, String reference) {
        Product product = productRepository.findById(productId, tenantId)
            .orElseThrow(() -> DomainException.notFound("Product", productId));
        product.receiveStock(quantity, unitCost);
        Product saved = productRepository.save(product);
        log.info("Stock received: product={}, qty={}, ref={}", productId, quantity, reference);
        return saved;
    }

    @Override
    public Product issueStock(UUID productId, String tenantId, BigDecimal quantity, String reference) {
        Product product = productRepository.findById(productId, tenantId)
            .orElseThrow(() -> DomainException.notFound("Product", productId));
        product.issueStock(quantity);
        Product saved = productRepository.save(product);

        if (saved.needsReorder()) {
            eventPublisher.publishLowStockAlert(new StockLowAlertEvent(
                tenantId, saved.getId(), saved.getProductCode(), saved.getName(),
                saved.getQuantityOnHand(), saved.getReorderPoint()));
        }
        log.info("Stock issued: product={}, qty={}, ref={}", productId, quantity, reference);
        return saved;
    }

    @Override
    public Product adjustStock(UUID productId, String tenantId, BigDecimal newQuantity, String reason) {
        Product product = productRepository.findById(productId, tenantId)
            .orElseThrow(() -> DomainException.notFound("Product", productId));
        BigDecimal diff = newQuantity.subtract(product.getQuantityOnHand());
        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            product.receiveStock(diff, product.getAverageCost());
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            product.issueStock(diff.abs());
        }
        Product saved = productRepository.save(product);
        log.info("Stock adjusted: product={}, newQty={}, reason={}", productId, newQuantity, reason);
        return saved;
    }
}
