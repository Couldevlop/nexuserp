package com.nexuserp.hr.application.command;

import com.nexuserp.hr.domain.model.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateEmployeeCommand(
    String tenantId,
    String employeeNumber,
    String firstName,
    String lastName,
    String email,
    String phone,
    String department,
    String jobTitle,
    Employee.ContractType contractType,
    LocalDate hireDate,
    BigDecimal grossSalary,
    String salaryCurrency,
    Employee.Country country,
    String socialSecurityNumber,
    String bankIban,
    String bankBic,
    String createdBy
) {}
