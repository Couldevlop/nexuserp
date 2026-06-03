package com.nexuserp.payment.adapter.in.rest;

import com.nexuserp.core.infrastructure.tenant.TenantContext;
import com.nexuserp.payment.application.command.InitiatePaymentCommand;
import com.nexuserp.payment.application.query.PaymentPageQuery;
import com.nexuserp.payment.domain.model.Payment;
import com.nexuserp.payment.domain.model.PaymentStatus;
import com.nexuserp.payment.domain.port.in.GetPaymentUseCase;
import com.nexuserp.payment.domain.port.in.InitiatePaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API d'orchestration des paiements Mobile Money.
 *
 * A01 (Broken Access Control) :
 *  - @PreAuthorize sur chaque endpoint authentifié,
 *  - toutes les opérations utilisent TenantContext (scoping tenant côté service/repo).
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Collecte & réconciliation Mobile Money (Orange, Wave, MTN, Moov)")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final InitiatePaymentUseCase initiatePaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final PaymentMapper mapper;

    public PaymentController(InitiatePaymentUseCase initiatePaymentUseCase,
                            GetPaymentUseCase getPaymentUseCase,
                            PaymentMapper mapper) {
        this.initiatePaymentUseCase = initiatePaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('FINANCE_USER', 'FINANCE_MANAGER', 'SALES_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Initier un paiement Mobile Money",
        description = "Crée un paiement PENDING et déclenche la collecte auprès du provider. "
            + "Le header Idempotency-Key prévient le double-débit (A04).")
    public ResponseEntity<PaymentDto> initiate(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();

        InitiatePaymentCommand command = mapper.toCommand(request, tenantId, userId, idempotencyKey);
        Payment payment = initiatePaymentUseCase.initiate(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(payment));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_USER', 'FINANCE_MANAGER', 'SALES_MANAGER', 'TENANT_ADMIN', 'AUDITOR')")
    @Operation(summary = "Obtenir un paiement par ID")
    public ResponseEntity<PaymentDto> getById(@PathVariable UUID id) {
        Payment payment = getPaymentUseCase.getById(id, TenantContext.getTenantId());
        return ResponseEntity.ok(mapper.toDto(payment));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_USER', 'FINANCE_MANAGER', 'SALES_MANAGER', 'TENANT_ADMIN', 'AUDITOR')")
    @Operation(summary = "Lister les paiements (paginé)", description = "Filtre disponible: status")
    public ResponseEntity<ApiPage<PaymentDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PaymentPageQuery query = new PaymentPageQuery(
            TenantContext.getTenantId(),
            status != null ? PaymentStatus.valueOf(status.toUpperCase()) : null,
            page, size, sortBy, sortDir
        );

        Page<Payment> result = getPaymentUseCase.getPayments(query);
        return ResponseEntity.ok(ApiPage.of(result.map(mapper::toDto)));
    }
}
