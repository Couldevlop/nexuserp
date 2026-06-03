package com.nexuserp.reporting.adapter.in.rest;

import com.nexuserp.reporting.domain.model.ReportRequest;
import com.nexuserp.reporting.domain.port.in.GenerateReportUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reporting", description = "Génération de rapports financiers et BI")
public class ReportingController {

    private final GenerateReportUseCase generateUseCase;

    public ReportingController(GenerateReportUseCase generateUseCase) {
        this.generateUseCase = generateUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'AUDITOR', 'TENANT_ADMIN')")
    @Operation(summary = "Générer un rapport (asynchrone)")
    public ResponseEntity<ReportDto> generate(
            @Valid @RequestBody GenerateReportRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = extractTenantId(jwt);
        ReportRequest domainReq = ReportRequest.builder()
            .tenantId(tenantId)
            .requestedBy(jwt.getSubject())
            .type(request.type())
            .periodFrom(request.periodFrom())
            .periodTo(request.periodTo())
            .outputFormat(request.outputFormat() != null
                ? request.outputFormat()
                : ReportRequest.OutputFormat.XLSX)
            .parameters(request.parameters())
            .build();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ReportDto.from(generateUseCase.generate(domainReq)));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'AUDITOR', 'TENANT_ADMIN', 'FINANCE_USER')")
    @Operation(summary = "Vérifier le statut d'un rapport")
    public ResponseEntity<ReportDto> getStatus(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ReportDto.from(generateUseCase.getStatus(id, extractTenantId(jwt))));
    }

    private String extractTenantId(Jwt jwt) {
        String t = jwt.getClaimAsString("tenantId");
        return t != null ? t : jwt.getSubject();
    }

    // DTOs
    public record GenerateReportRequest(
        @NotNull ReportRequest.ReportType type,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
        ReportRequest.OutputFormat outputFormat,
        Map<String, String> parameters
    ) {}

    public record ReportDto(
        String id,
        String type,
        String status,
        String downloadUrl,
        String errorMessage,
        String requestedAt,
        String completedAt
    ) {
        public static ReportDto from(ReportRequest r) {
            return new ReportDto(
                r.getId(),
                r.getType().name(),
                r.getStatus() != null ? r.getStatus().name() : "PENDING",
                r.getDownloadUrl(),
                r.getErrorMessage(),
                r.getRequestedAt() != null ? r.getRequestedAt().toString() : null,
                r.getCompletedAt() != null ? r.getCompletedAt().toString() : null
            );
        }
    }
}
