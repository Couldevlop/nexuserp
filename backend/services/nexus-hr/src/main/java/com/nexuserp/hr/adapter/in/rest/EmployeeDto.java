package com.nexuserp.hr.adapter.in.rest;

import com.nexuserp.hr.domain.model.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeDto(
    UUID id,
    String tenantId,
    String employeeNumber,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String phone,
    String department,
    String jobTitle,
    String contractType,
    String status,
    LocalDate hireDate,
    BigDecimal grossSalaryAmount,
    String grossSalaryCurrency,
    String country
) {
    public static EmployeeDto from(Employee e) {
        return new EmployeeDto(
            e.getId(), e.getTenantId().value(), e.getEmployeeNumber(),
            e.getFirstName(), e.getLastName(), e.getFullName(),
            e.getEmail(), e.getPhone(), e.getDepartment(), e.getJobTitle(),
            e.getContractType().name(), e.getStatus().name(), e.getHireDate(),
            e.getGrossSalary().amount(), e.getGrossSalary().currency(),
            e.getCountry().name()
        );
    }
}
