package com.nexuserp.importservice.adapter.in.rest;

import com.nexuserp.importservice.domain.model.ImportResult;
import com.nexuserp.importservice.infrastructure.importer.CustomerImporter;
import com.nexuserp.importservice.infrastructure.importer.EmployeeImporter;
import com.nexuserp.importservice.infrastructure.importer.ProductImporter;
import com.nexuserp.importservice.infrastructure.importer.SupplierImporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/import")
@Tag(name = "Import", description = "Import de données depuis fichiers XLSX")
public class ImportController {

    private final ProductImporter productImporter;
    private final SupplierImporter supplierImporter;
    private final CustomerImporter customerImporter;
    private final EmployeeImporter employeeImporter;

    public ImportController(ProductImporter productImporter,
                            SupplierImporter supplierImporter,
                            CustomerImporter customerImporter,
                            EmployeeImporter employeeImporter) {
        this.productImporter = productImporter;
        this.supplierImporter = supplierImporter;
        this.customerImporter = customerImporter;
        this.employeeImporter = employeeImporter;
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'PROCUREMENT_MANAGER', 'PRODUCTION_MANAGER')")
    @Operation(summary = "Importer des articles/produits depuis XLSX")
    public ResponseEntity<ImportResultDto> importProducts(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(ImportResultDto.from(productImporter.process(file, tenantId)));
    }

    @PostMapping(value = "/suppliers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Importer des fournisseurs depuis XLSX")
    public ResponseEntity<ImportResultDto> importSuppliers(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(ImportResultDto.from(supplierImporter.process(file, tenantId)));
    }

    @PostMapping(value = "/customers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_MANAGER')")
    @Operation(summary = "Importer des clients depuis XLSX")
    public ResponseEntity<ImportResultDto> importCustomers(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(ImportResultDto.from(customerImporter.process(file, tenantId)));
    }

    @PostMapping(value = "/employees", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Importer des salariés depuis XLSX")
    public ResponseEntity<ImportResultDto> importEmployees(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(ImportResultDto.from(employeeImporter.process(file, tenantId)));
    }

    private String extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenantId");
        return tenantId != null ? tenantId : jwt.getClaimAsString("sub");
    }

    public record ImportResultDto(
        int totalRows,
        int successRows,
        int errorRows,
        boolean hasErrors,
        java.util.List<ErrorDto> errors,
        String downloadableErrorReport
    ) {
        public static ImportResultDto from(ImportResult<?> result) {
            return new ImportResultDto(
                result.totalRows(),
                result.successRows(),
                result.errorRows(),
                result.hasErrors(),
                result.errors().stream().map(e -> new ErrorDto(
                    e.rowNumber(), e.column(), String.valueOf(e.rejectedValue()), e.message()
                )).toList(),
                result.downloadableErrorReport()
            );
        }
    }

    public record ErrorDto(int row, String column, String rejectedValue, String message) {}
}
