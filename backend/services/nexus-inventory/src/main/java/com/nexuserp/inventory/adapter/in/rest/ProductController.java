package com.nexuserp.inventory.adapter.in.rest;

import com.nexuserp.core.infrastructure.tenant.TenantContext;
import com.nexuserp.inventory.application.command.CreateProductCommand;
import com.nexuserp.inventory.application.query.ProductPageQuery;
import com.nexuserp.inventory.domain.model.Product;
import com.nexuserp.inventory.domain.port.in.CreateProductUseCase;
import com.nexuserp.inventory.domain.port.in.GetProductUseCase;
import com.nexuserp.inventory.domain.port.in.StockMovementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/products")
@Tag(name = "Inventory", description = "Gestion des stocks et produits")
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final StockMovementUseCase stockMovementUseCase;

    public ProductController(CreateProductUseCase createProductUseCase,
                              GetProductUseCase getProductUseCase,
                              StockMovementUseCase stockMovementUseCase) {
        this.createProductUseCase = createProductUseCase;
        this.getProductUseCase = getProductUseCase;
        this.stockMovementUseCase = stockMovementUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Créer un produit/article")
    public ResponseEntity<ProductDto> create(@Valid @RequestBody CreateProductRequest request) {
        String tenantId = TenantContext.getTenantId();
        CreateProductCommand cmd = new CreateProductCommand(
            tenantId, request.productCode(), request.name(), request.description(),
            request.category(), request.unit(), request.reorderPoint(), request.reorderQuantity(),
            request.safetyStock(), request.valuationMethod(), request.warehouseId(),
            request.warehouseLocation(), request.serialTracked(), request.lotTracked(),
            request.expiryTracked(), "system"
        );
        Product product = createProductUseCase.createProduct(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductDto.from(product));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'INVENTORY_USER', 'TENANT_ADMIN')")
    @Operation(summary = "Obtenir un produit par ID")
    public ResponseEntity<ProductDto> getById(@PathVariable UUID id) {
        Product product = getProductUseCase.getById(id, TenantContext.getTenantId());
        return ResponseEntity.ok(ProductDto.from(product));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'INVENTORY_USER', 'TENANT_ADMIN')")
    @Operation(summary = "Lister les produits (paginé)")
    public ResponseEntity<Page<ProductDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        ProductPageQuery query = new ProductPageQuery(TenantContext.getTenantId(), category, page, size, sort, dir);
        return ResponseEntity.ok(getProductUseCase.getProducts(query).map(ProductDto::from));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Enregistrer une entrée de stock")
    public ResponseEntity<ProductDto> receiveStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockReceiveRequest request) {
        Product product = stockMovementUseCase.receiveStock(
            id, TenantContext.getTenantId(), request.quantity(),
            com.nexuserp.core.domain.value.Money.of(request.unitCost(), request.currency()),
            request.reference()
        );
        return ResponseEntity.ok(ProductDto.from(product));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Enregistrer une sortie de stock")
    public ResponseEntity<ProductDto> issueStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockIssueRequest request) {
        Product product = stockMovementUseCase.issueStock(
            id, TenantContext.getTenantId(), request.quantity(), request.reference()
        );
        return ResponseEntity.ok(ProductDto.from(product));
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    public record CreateProductRequest(
        @NotNull String productCode,
        @NotNull String name,
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
        boolean expiryTracked
    ) {}

    public record StockReceiveRequest(
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal unitCost,
        @NotNull String currency,
        String reference
    ) {}

    public record StockIssueRequest(
        @NotNull @Positive BigDecimal quantity,
        String reference
    ) {}
}
