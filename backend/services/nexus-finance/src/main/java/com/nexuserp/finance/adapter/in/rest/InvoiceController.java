package com.nexuserp.finance.adapter.in.rest;

import com.nexuserp.core.infrastructure.tenant.TenantContext;
import com.nexuserp.finance.application.command.CreateInvoiceCommand;
import com.nexuserp.finance.application.query.InvoicePageQuery;
import com.nexuserp.finance.domain.model.Invoice;
import com.nexuserp.finance.domain.port.in.ApproveInvoiceUseCase;
import com.nexuserp.finance.domain.port.in.CreateInvoiceUseCase;
import com.nexuserp.finance.domain.port.in.GetInvoiceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/invoices")
@Tag(name = "Invoices", description = "Gestion des factures — PCG France & SYSCOHADA")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final GetInvoiceUseCase getInvoiceUseCase;
    private final ApproveInvoiceUseCase approveInvoiceUseCase;
    private final InvoiceMapper invoiceMapper;

    public InvoiceController(CreateInvoiceUseCase createInvoiceUseCase,
                              GetInvoiceUseCase getInvoiceUseCase,
                              ApproveInvoiceUseCase approveInvoiceUseCase,
                              InvoiceMapper invoiceMapper) {
        this.createInvoiceUseCase = createInvoiceUseCase;
        this.getInvoiceUseCase = getInvoiceUseCase;
        this.approveInvoiceUseCase = approveInvoiceUseCase;
        this.invoiceMapper = invoiceMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('FINANCE_USER', 'FINANCE_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Créer une facture", description = "Crée une nouvelle facture (client ou fournisseur)")
    public ResponseEntity<InvoiceDto> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();

        CreateInvoiceCommand command = invoiceMapper.toCommand(request, tenantId, userId);
        Invoice invoice = createInvoiceUseCase.createInvoice(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceMapper.toDto(invoice));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_USER', 'FINANCE_MANAGER', 'TENANT_ADMIN', 'AUDITOR')")
    @Operation(summary = "Obtenir une facture par ID")
    public ResponseEntity<InvoiceDto> getInvoice(@PathVariable UUID id) {
        Invoice invoice = getInvoiceUseCase.getById(id, TenantContext.getTenantId());
        return ResponseEntity.ok(invoiceMapper.toDto(invoice));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_USER', 'FINANCE_MANAGER', 'TENANT_ADMIN', 'AUDITOR')")
    @Operation(summary = "Lister les factures (paginé)", description = "Filtres disponibles: status, dateFrom, dateTo, partnerName")
    public ResponseEntity<ApiPage<InvoiceDto>> listInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String partnerName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "invoiceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        InvoicePageQuery query = new InvoicePageQuery(
            TenantContext.getTenantId(),
            status != null ? Invoice.InvoiceStatus.valueOf(status.toUpperCase()) : null,
            dateFrom != null ? LocalDate.parse(dateFrom) : null,
            dateTo != null ? LocalDate.parse(dateTo) : null,
            partnerName, page, size, sortBy, sortDir
        );

        Page<Invoice> invoicePage = getInvoiceUseCase.getInvoices(query);
        return ResponseEntity.ok(ApiPage.of(invoicePage.map(invoiceMapper::toDto)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Valider une facture", description = "Réservé aux FINANCE_MANAGER")
    public ResponseEntity<InvoiceDto> approveInvoice(@PathVariable UUID id) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();
        Invoice invoice = approveInvoiceUseCase.approveInvoice(id, tenantId, userId);
        return ResponseEntity.ok(invoiceMapper.toDto(invoice));
    }
}
