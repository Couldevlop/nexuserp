package com.nexuserp.sales.adapter.in.rest;

import com.nexuserp.core.infrastructure.tenant.TenantContext;
import com.nexuserp.sales.application.command.CreateSalesOrderCommand;
import com.nexuserp.sales.domain.model.SalesOrder;
import com.nexuserp.sales.domain.model.SalesOrderLine;
import com.nexuserp.sales.domain.port.in.CreateSalesOrderUseCase;
import com.nexuserp.sales.domain.port.in.GetSalesOrderUseCase;
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
@RequestMapping("/api/v1/sales/orders")
@Tag(name = "Sales", description = "Gestion des ventes et commandes clients")
public class SalesOrderController {

    private final CreateSalesOrderUseCase createUseCase;
    private final GetSalesOrderUseCase getUseCase;

    public SalesOrderController(CreateSalesOrderUseCase createUseCase, GetSalesOrderUseCase getUseCase) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SALES_MANAGER', 'SALES_USER', 'TENANT_ADMIN')")
    @Operation(summary = "Créer une commande client")
    public ResponseEntity<SalesOrderDto> create(@Valid @RequestBody CreateSORequest request) {
        String tenantId = TenantContext.getTenantId();
        List<SalesOrderLine.LineData> lines = request.lines() != null
            ? request.lines().stream().map(l -> new SalesOrderLine.LineData(
                l.productCode(), l.productName(), l.quantity(), l.unitPrice(), l.taxRate())).toList()
            : List.of();

        CreateSalesOrderCommand cmd = new CreateSalesOrderCommand(
            tenantId, request.customerId(), request.customerName(), request.customerRef(),
            request.requestedDeliveryDate(), request.currency(), request.shippingAddress(),
            request.notes(), lines, "system"
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(SalesOrderDto.from(createUseCase.createSalesOrder(cmd)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SALES_MANAGER', 'SALES_USER', 'TENANT_ADMIN')")
    @Operation(summary = "Obtenir une commande client")
    public ResponseEntity<SalesOrderDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(SalesOrderDto.from(getUseCase.getById(id, TenantContext.getTenantId())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SALES_MANAGER', 'SALES_USER', 'TENANT_ADMIN')")
    @Operation(summary = "Lister les commandes clients")
    public ResponseEntity<Page<SalesOrderDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(getUseCase.list(TenantContext.getTenantId(), status,
            PageRequest.of(page, size)).map(SalesOrderDto::from));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('SALES_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Confirmer une commande client")
    public ResponseEntity<SalesOrderDto> confirm(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(SalesOrderDto.from(
            createUseCase.confirmSalesOrder(id, TenantContext.getTenantId(), body.get("confirmedBy"))));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SALES_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Annuler une commande client")
    public ResponseEntity<SalesOrderDto> cancel(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(SalesOrderDto.from(
            createUseCase.cancelSalesOrder(id, TenantContext.getTenantId(), body.get("reason"))));
    }

    // DTOs
    public record CreateSORequest(
        UUID customerId,
        @NotNull String customerName,
        String customerRef,
        LocalDate requestedDeliveryDate,
        String currency,
        String shippingAddress,
        String notes,
        List<LineRequest> lines
    ) {}

    public record LineRequest(
        String productCode, @NotNull String productName,
        @NotNull BigDecimal quantity, @NotNull BigDecimal unitPrice, BigDecimal taxRate
    ) {}
}
