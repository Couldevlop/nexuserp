package com.nexuserp.production.adapter.in.rest;

import com.nexuserp.core.infrastructure.tenant.TenantContext;
import com.nexuserp.production.application.command.CreateWorkOrderCommand;
import com.nexuserp.production.domain.model.WorkOrder;
import com.nexuserp.production.domain.port.in.CreateWorkOrderUseCase;
import com.nexuserp.production.domain.port.in.GetWorkOrderUseCase;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/production/work-orders")
@Tag(name = "Production", description = "Gestion des ordres de fabrication")
public class WorkOrderController {

    private final CreateWorkOrderUseCase createUseCase;
    private final GetWorkOrderUseCase getUseCase;

    public WorkOrderController(CreateWorkOrderUseCase createUseCase, GetWorkOrderUseCase getUseCase) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PRODUCTION_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Créer un ordre de fabrication")
    public ResponseEntity<WorkOrderDto> create(@Valid @RequestBody CreateWORequest request) {
        CreateWorkOrderCommand cmd = new CreateWorkOrderCommand(
            TenantContext.getTenantId(), request.productId(), request.productName(),
            request.quantityPlanned(), request.plannedStartDate(), request.plannedEndDate(),
            request.priority() != null ? request.priority() : WorkOrder.Priority.NORMAL,
            request.workcenter(), request.bomId(), request.routingId(), request.notes(), "system"
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(WorkOrderDto.from(createUseCase.createWorkOrder(cmd)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRODUCTION_MANAGER', 'PRODUCTION_USER', 'TENANT_ADMIN')")
    @Operation(summary = "Obtenir un ordre de fabrication")
    public ResponseEntity<WorkOrderDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(WorkOrderDto.from(getUseCase.getById(id, TenantContext.getTenantId())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PRODUCTION_MANAGER', 'PRODUCTION_USER', 'TENANT_ADMIN')")
    @Operation(summary = "Lister les ordres de fabrication")
    public ResponseEntity<Page<WorkOrderDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(getUseCase.list(TenantContext.getTenantId(), status,
            PageRequest.of(page, size)).map(WorkOrderDto::from));
    }

    @PutMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('PRODUCTION_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Lancer un ordre de fabrication")
    public ResponseEntity<WorkOrderDto> release(@PathVariable UUID id) {
        return ResponseEntity.ok(WorkOrderDto.from(createUseCase.releaseWorkOrder(id, TenantContext.getTenantId())));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('PRODUCTION_MANAGER', 'PRODUCTION_USER', 'TENANT_ADMIN')")
    @Operation(summary = "Démarrer la production")
    public ResponseEntity<WorkOrderDto> start(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(WorkOrderDto.from(
            createUseCase.startWorkOrder(id, TenantContext.getTenantId(), body.get("operatorId"))));
    }

    @PutMapping("/{id}/production")
    @PreAuthorize("hasAnyRole('PRODUCTION_MANAGER', 'PRODUCTION_USER', 'TENANT_ADMIN')")
    @Operation(summary = "Enregistrer la production")
    public ResponseEntity<WorkOrderDto> recordProduction(
            @PathVariable UUID id, @Valid @RequestBody ProductionRecordRequest request) {
        return ResponseEntity.ok(WorkOrderDto.from(
            createUseCase.recordProduction(id, TenantContext.getTenantId(),
                request.quantity(), request.rejected(), request.operatorId())));
    }

    // DTOs
    public record CreateWORequest(
        String productId,
        @NotNull String productName,
        @NotNull BigDecimal quantityPlanned,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        WorkOrder.Priority priority,
        String workcenter,
        String bomId,
        String routingId,
        String notes
    ) {}

    public record ProductionRecordRequest(
        @NotNull BigDecimal quantity,
        BigDecimal rejected,
        String operatorId
    ) {}
}
