package com.nexuserp.hr.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees", schema = "nexus_hr",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "employee_number"}))
public class EmployeeJpaEntity {

    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
    @Column(name = "employee_number", nullable = false, length = 30) private String employeeNumber;
    @Column(name = "first_name", nullable = false) private String firstName;
    @Column(name = "last_name", nullable = false) private String lastName;
    @Column(name = "email") private String email;
    @Column(name = "phone", length = 30) private String phone;
    @Column(name = "department", length = 100) private String department;
    @Column(name = "job_title") private String jobTitle;
    @Column(name = "contract_type", length = 20) private String contractType;
    @Column(name = "status", length = 20) private String status;
    @Column(name = "hire_date") private LocalDate hireDate;
    @Column(name = "termination_date") private LocalDate terminationDate;
    @Column(name = "gross_salary_amount", precision = 19, scale = 4) private BigDecimal grossSalaryAmount;
    @Column(name = "gross_salary_currency", length = 3) private String grossSalaryCurrency;
    @Column(name = "country", length = 10) private String country;
    @Column(name = "social_security_number", length = 50) private String socialSecurityNumber;
    @Column(name = "bank_iban", length = 34) private String bankIban;
    @Column(name = "bank_bic", length = 11) private String bankBic;
    @Column(name = "created_at", updatable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = Instant.now(); }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    // Getters & Setters
    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public String getTenantId() { return tenantId; } public void setTenantId(String t) { this.tenantId = t; }
    public String getEmployeeNumber() { return employeeNumber; } public void setEmployeeNumber(String n) { this.employeeNumber = n; }
    public String getFirstName() { return firstName; } public void setFirstName(String n) { this.firstName = n; }
    public String getLastName() { return lastName; } public void setLastName(String n) { this.lastName = n; }
    public String getEmail() { return email; } public void setEmail(String e) { this.email = e; }
    public String getPhone() { return phone; } public void setPhone(String p) { this.phone = p; }
    public String getDepartment() { return department; } public void setDepartment(String d) { this.department = d; }
    public String getJobTitle() { return jobTitle; } public void setJobTitle(String j) { this.jobTitle = j; }
    public String getContractType() { return contractType; } public void setContractType(String c) { this.contractType = c; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public LocalDate getHireDate() { return hireDate; } public void setHireDate(LocalDate d) { this.hireDate = d; }
    public LocalDate getTerminationDate() { return terminationDate; } public void setTerminationDate(LocalDate d) { this.terminationDate = d; }
    public BigDecimal getGrossSalaryAmount() { return grossSalaryAmount; } public void setGrossSalaryAmount(BigDecimal a) { this.grossSalaryAmount = a; }
    public String getGrossSalaryCurrency() { return grossSalaryCurrency; } public void setGrossSalaryCurrency(String c) { this.grossSalaryCurrency = c; }
    public String getCountry() { return country; } public void setCountry(String c) { this.country = c; }
    public String getSocialSecurityNumber() { return socialSecurityNumber; } public void setSocialSecurityNumber(String s) { this.socialSecurityNumber = s; }
    public String getBankIban() { return bankIban; } public void setBankIban(String i) { this.bankIban = i; }
    public String getBankBic() { return bankBic; } public void setBankBic(String b) { this.bankBic = b; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
