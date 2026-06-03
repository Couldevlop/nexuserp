package com.nexuserp.hr.domain.port.out;

import com.nexuserp.hr.domain.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository {
    Employee save(Employee employee);
    Optional<Employee> findById(UUID id, String tenantId);
    Optional<Employee> findByEmployeeNumber(String employeeNumber, String tenantId);
    Page<Employee> findAll(String tenantId, String department, Pageable pageable);
    long countActive(String tenantId);
}
