package com.nexuserp.hr.domain.service;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.Money;
import com.nexuserp.hr.application.command.CreateEmployeeCommand;
import com.nexuserp.hr.application.command.CreateLeaveCommand;
import com.nexuserp.hr.application.query.EmployeePageQuery;
import com.nexuserp.hr.domain.model.Employee;
import com.nexuserp.hr.domain.model.Leave;
import com.nexuserp.hr.domain.port.in.CreateEmployeeUseCase;
import com.nexuserp.hr.domain.port.in.GetEmployeeUseCase;
import com.nexuserp.hr.domain.port.in.ProcessLeaveUseCase;
import com.nexuserp.hr.domain.port.out.EmployeeRepository;
import com.nexuserp.hr.domain.port.out.LeaveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class HrService implements CreateEmployeeUseCase, GetEmployeeUseCase, ProcessLeaveUseCase {

    private static final Logger log = LoggerFactory.getLogger(HrService.class);

    private final EmployeeRepository employeeRepository;
    private final LeaveRepository leaveRepository;
    private final PayrollCalculator payrollCalculator;

    public HrService(EmployeeRepository employeeRepository,
                     LeaveRepository leaveRepository,
                     PayrollCalculator payrollCalculator) {
        this.employeeRepository = employeeRepository;
        this.leaveRepository = leaveRepository;
        this.payrollCalculator = payrollCalculator;
    }

    @Override
    public Employee createEmployee(CreateEmployeeCommand cmd) {
        if (employeeRepository.findByEmployeeNumber(cmd.employeeNumber(), cmd.tenantId()).isPresent()) {
            throw DomainException.of("DUPLICATE_EMPLOYEE_NUMBER",
                "Employee number already exists: " + cmd.employeeNumber());
        }
        Employee employee = Employee.builder()
            .tenantId(cmd.tenantId())
            .employeeNumber(cmd.employeeNumber())
            .firstName(cmd.firstName())
            .lastName(cmd.lastName())
            .email(cmd.email())
            .phone(cmd.phone())
            .department(cmd.department())
            .jobTitle(cmd.jobTitle())
            .contractType(cmd.contractType())
            .hireDate(cmd.hireDate())
            .grossSalary(Money.of(cmd.grossSalary(), cmd.salaryCurrency()))
            .country(cmd.country())
            .socialSecurityNumber(cmd.socialSecurityNumber())
            .bankIban(cmd.bankIban())
            .bankBic(cmd.bankBic())
            .build();

        Employee saved = employeeRepository.save(employee);
        log.info("Employee created: number={}, tenant={}", saved.getEmployeeNumber(), cmd.tenantId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getById(UUID id, String tenantId) {
        return employeeRepository.findById(id, tenantId)
            .orElseThrow(() -> DomainException.notFound("Employee", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> getEmployees(EmployeePageQuery query) {
        return employeeRepository.findAll(query.tenantId(), query.department(), query.toPageable());
    }

    @Override
    public Leave requestLeave(CreateLeaveCommand cmd) {
        // Vérifier que l'employé existe
        employeeRepository.findById(cmd.employeeId(), cmd.tenantId())
            .orElseThrow(() -> DomainException.notFound("Employee", cmd.employeeId()));

        Leave leave = Leave.builder()
            .tenantId(cmd.tenantId())
            .employeeId(cmd.employeeId())
            .leaveType(cmd.leaveType())
            .startDate(cmd.startDate())
            .endDate(cmd.endDate())
            .reason(cmd.reason())
            .build();

        leave.submit();
        Leave saved = leaveRepository.save(leave);
        log.info("Leave requested: employee={}, type={}, days={}", cmd.employeeId(), cmd.leaveType(), saved.getDurationDays());
        return saved;
    }

    @Override
    public Leave approveLeave(UUID leaveId, String tenantId, String approvedBy) {
        Leave leave = leaveRepository.findById(leaveId, tenantId)
            .orElseThrow(() -> DomainException.notFound("Leave", leaveId));
        leave.approve(approvedBy);
        return leaveRepository.save(leave);
    }

    @Override
    public Leave rejectLeave(UUID leaveId, String tenantId, String rejectedBy, String reason) {
        Leave leave = leaveRepository.findById(leaveId, tenantId)
            .orElseThrow(() -> DomainException.notFound("Leave", leaveId));
        leave.reject(rejectedBy, reason);
        return leaveRepository.save(leave);
    }

    public PayrollCalculator.PayslipResult calculatePayslip(UUID employeeId, String tenantId) {
        Employee employee = employeeRepository.findById(employeeId, tenantId)
            .orElseThrow(() -> DomainException.notFound("Employee", employeeId));
        return payrollCalculator.calculate(employee);
    }
}
