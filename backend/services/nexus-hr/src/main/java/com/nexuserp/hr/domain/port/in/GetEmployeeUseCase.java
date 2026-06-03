package com.nexuserp.hr.domain.port.in;

import com.nexuserp.hr.application.query.EmployeePageQuery;
import com.nexuserp.hr.domain.model.Employee;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface GetEmployeeUseCase {
    Employee getById(UUID id, String tenantId);
    Page<Employee> getEmployees(EmployeePageQuery query);
}
