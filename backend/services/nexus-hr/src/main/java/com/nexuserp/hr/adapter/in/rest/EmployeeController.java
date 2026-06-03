package com.nexuserp.hr.adapter.in.rest;

import com.nexuserp.core.infrastructure.tenant.TenantContext;
import com.nexuserp.hr.application.command.CreateEmployeeCommand;
import com.nexuserp.hr.application.command.CreateLeaveCommand;
import com.nexuserp.hr.application.query.EmployeePageQuery;
import com.nexuserp.hr.domain.model.Employee;
import com.nexuserp.hr.domain.model.Leave;
import com.nexuserp.hr.domain.service.HrService;
import com.nexuserp.hr.domain.service.PayrollCalculator;
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
@RequestMapping("/api/v1/hr")
@Tag(name = "HR", description = "Ressources Humaines — salariés, congés, paie")
public class EmployeeController {

    private final HrService hrService;

    public EmployeeController(HrService hrService) {
        this.hrService = hrService;
    }

    @PostMapping("/employees")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Créer un salarié")
    public ResponseEntity<EmployeeDto> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        String tenantId = TenantContext.getTenantId();
        CreateEmployeeCommand cmd = new CreateEmployeeCommand(
            tenantId, request.employeeNumber(), request.firstName(), request.lastName(),
            request.email(), request.phone(), request.department(), request.jobTitle(),
            request.contractType(), request.hireDate(), request.grossSalary(),
            request.salaryCurrency(), request.country(), request.socialSecurityNumber(),
            request.bankIban(), request.bankBic(), "system"
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeDto.from(hrService.createEmployee(cmd)));
    }

    @GetMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('HR_USER', 'HR_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Obtenir un salarié")
    public ResponseEntity<EmployeeDto> getEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(EmployeeDto.from(hrService.getById(id, TenantContext.getTenantId())));
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('HR_USER', 'HR_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Lister les salariés")
    public ResponseEntity<Page<EmployeeDto>> listEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String department) {
        EmployeePageQuery query = new EmployeePageQuery(TenantContext.getTenantId(), department, page, size, "lastName", "asc");
        return ResponseEntity.ok(hrService.getEmployees(query).map(EmployeeDto::from));
    }

    @PostMapping("/leaves")
    @PreAuthorize("hasAnyRole('HR_USER', 'HR_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Demander un congé")
    public ResponseEntity<LeaveDto> requestLeave(@Valid @RequestBody CreateLeaveRequest request) {
        String tenantId = TenantContext.getTenantId();
        CreateLeaveCommand cmd = new CreateLeaveCommand(
            tenantId, request.employeeId(), request.leaveType(),
            request.startDate(), request.endDate(), request.reason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(LeaveDto.from(hrService.requestLeave(cmd)));
    }

    @PutMapping("/leaves/{id}/approve")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Approuver un congé")
    public ResponseEntity<LeaveDto> approveLeave(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        Leave leave = hrService.approveLeave(id, TenantContext.getTenantId(), body.get("approvedBy"));
        return ResponseEntity.ok(LeaveDto.from(leave));
    }

    @PutMapping("/leaves/{id}/reject")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Rejeter un congé")
    public ResponseEntity<LeaveDto> rejectLeave(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        Leave leave = hrService.rejectLeave(id, TenantContext.getTenantId(), body.get("rejectedBy"), body.get("reason"));
        return ResponseEntity.ok(LeaveDto.from(leave));
    }

    @GetMapping("/employees/{id}/payslip")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'TENANT_ADMIN')")
    @Operation(summary = "Simuler un bulletin de paie")
    public ResponseEntity<PayrollCalculator.PayslipResult> getPayslip(@PathVariable UUID id) {
        return ResponseEntity.ok(hrService.calculatePayslip(id, TenantContext.getTenantId()));
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    public record CreateEmployeeRequest(
        @NotNull String employeeNumber,
        @NotNull String firstName,
        @NotNull String lastName,
        String email,
        String phone,
        String department,
        String jobTitle,
        @NotNull Employee.ContractType contractType,
        @NotNull LocalDate hireDate,
        @NotNull BigDecimal grossSalary,
        @NotNull String salaryCurrency,
        @NotNull Employee.Country country,
        String socialSecurityNumber,
        String bankIban,
        String bankBic
    ) {}

    public record CreateLeaveRequest(
        @NotNull UUID employeeId,
        @NotNull Leave.LeaveType leaveType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason
    ) {}
}
