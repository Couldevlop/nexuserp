package com.nexuserp.hr.adapter.out.persistence;

import com.nexuserp.core.domain.value.Money;
import com.nexuserp.hr.domain.model.Employee;
import com.nexuserp.hr.domain.port.out.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final EmployeeJpaRepository jpaRepository;

    public EmployeeRepositoryAdapter(EmployeeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Employee save(Employee e) {
        return toDomain(jpaRepository.save(toJpa(e)));
    }

    @Override
    public Optional<Employee> findById(UUID id, String tenantId) {
        return jpaRepository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public Optional<Employee> findByEmployeeNumber(String number, String tenantId) {
        return jpaRepository.findByEmployeeNumberAndTenantId(number, tenantId).map(this::toDomain);
    }

    @Override
    public Page<Employee> findAll(String tenantId, String department, Pageable pageable) {
        if (department != null && !department.isBlank()) {
            return jpaRepository.findByTenantIdAndDepartment(tenantId, department, pageable).map(this::toDomain);
        }
        return jpaRepository.findByTenantId(tenantId, pageable).map(this::toDomain);
    }

    @Override
    public long countActive(String tenantId) {
        return jpaRepository.countByTenantIdAndStatus(tenantId, "ACTIVE");
    }

    private EmployeeJpaEntity toJpa(Employee e) {
        EmployeeJpaEntity entity = new EmployeeJpaEntity();
        entity.setId(e.getId());
        entity.setTenantId(e.getTenantId().value());
        entity.setEmployeeNumber(e.getEmployeeNumber());
        entity.setFirstName(e.getFirstName());
        entity.setLastName(e.getLastName());
        entity.setEmail(e.getEmail());
        entity.setPhone(e.getPhone());
        entity.setDepartment(e.getDepartment());
        entity.setJobTitle(e.getJobTitle());
        entity.setContractType(e.getContractType().name());
        entity.setStatus(e.getStatus().name());
        entity.setHireDate(e.getHireDate());
        entity.setTerminationDate(e.getTerminationDate());
        entity.setGrossSalaryAmount(e.getGrossSalary().amount());
        entity.setGrossSalaryCurrency(e.getGrossSalary().currency());
        entity.setCountry(e.getCountry().name());
        entity.setSocialSecurityNumber(e.getSocialSecurityNumber());
        entity.setBankIban(e.getBankIban());
        entity.setBankBic(e.getBankBic());
        return entity;
    }

    private Employee toDomain(EmployeeJpaEntity e) {
        return Employee.builder()
            .id(e.getId())
            .tenantId(e.getTenantId())
            .employeeNumber(e.getEmployeeNumber())
            .firstName(e.getFirstName())
            .lastName(e.getLastName())
            .email(e.getEmail())
            .phone(e.getPhone())
            .department(e.getDepartment())
            .jobTitle(e.getJobTitle())
            .contractType(Employee.ContractType.valueOf(e.getContractType()))
            .hireDate(e.getHireDate())
            .grossSalary(Money.of(e.getGrossSalaryAmount(), e.getGrossSalaryCurrency()))
            .country(Employee.Country.valueOf(e.getCountry()))
            .socialSecurityNumber(e.getSocialSecurityNumber())
            .bankIban(e.getBankIban())
            .bankBic(e.getBankBic())
            .build();
    }
}
