package com.nexuserp.procurement.adapter.in.rest;

import com.nexuserp.core.infrastructure.tenant.TenantContext;
import com.nexuserp.procurement.application.command.CreatePurchaseOrderCommand;
import com.nexuserp.procurement.domain.model.PurchaseOrder;
import com.nexuserp.procurement.domain.model.PurchaseOrderLine;
import com.nexuserp.procurement.domain.port.in.CreatePurchaseOrderUseCase;
import com.nexuserp.procurement.domain.port.in.GetPurchaseOrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/procurement/purchase-orders")
@Tag(name = "Procurement", description = "Gestion des achats et commandes fournisseurs")
public class PurchaseOrderController {

    private final CreatePurchaseOrderUseCase createUseCase;
    private final GetPurchaseOrderUseCase getUseCase;

    public PurchaseOrderController(CreatePurchaseOrderUseCase createUseCase, GetPurchaseOrderUseCase getUseCase) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Créer une commande fournisseur")
    public ResponseEntity<PurchaseOrderDto> create(@Valid @RequestBody CreatePORequest request) {
        String tenantId = TenantContext.getTenantId();
        List<PurchaseOrderLine.LineData> lines = request.lines() != null
            ? request.lines().stream().map(l -> new PurchaseOrderLine.LineData(
                l.productCode(), l.description(), l.quantity(), l.unitPrice(), l.taxRate())).toList()
            : List.of();

        CreatePurchaseOrderCommand cmd = new CreatePurchaseOrderCommand(
            tenantId, request.supplierId(), request.supplierName(),
            request.expectedDeliveryDate(), request.currency(), request.notes(), lines, "system"
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseOrderDto.from(createUseCase.createPurchaseOrder(cmd)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_USER', 'TENANT_ADMIN')")
    @Operation(summary = "Obtenir une commande fournisseur")
    public ResponseEntity<PurchaseOrderDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(PurchaseOrderDto.from(getUseCase.getById(id, TenantContext.getTenantId())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'PROCUREMENT_USER', 'TENANT_ADMIN')")
    @Operation(summary = "Lister les commandes fournisseurs")
    public ResponseEntity<Page<PurchaseOrderDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(getUseCase.list(TenantContext.getTenantId(), status,
            PageRequest.of(page, size)).map(PurchaseOrderDto::from));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Approuver une commande fournisseur")
    public ResponseEntity<PurchaseOrderDto> approve(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(PurchaseOrderDto.from(
            createUseCase.approvePurchaseOrder(id, TenantContext.getTenantId(), body.get("approvedBy"))));
    }

    // DTOs
    public record CreatePORequest(
        UUID supplierId,
        @NotNull String supplierName,
        LocalDate expectedDeliveryDate,
        String currency,
        String notes,
        List<LineRequest> lines
    ) {}

    public record LineRequest(
        String productCode, @NotNull String description,
        @NotNull BigDecimal quantity, @NotNull BigDecimal unitPrice, BigDecimal taxRate
    ) {}
}
