package com.nexuserp.hr.domain.port.in;

import com.nexuserp.hr.application.command.CreateEmployeeCommand;
import com.nexuserp.hr.domain.model.Employee;

public interface CreateEmployeeUseCase {
    Employee createEmployee(CreateEmployeeCommand command);
}
