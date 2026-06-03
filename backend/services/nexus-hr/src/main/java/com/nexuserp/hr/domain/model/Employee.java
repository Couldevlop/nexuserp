package com.nexuserp.hr.domain.model;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.Money;
import com.nexuserp.core.domain.value.TenantId;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Agrégat Employé — Module RH.
 * Gère : contrat, salaire, congés, absences, paie.
 */
public class Employee {

    public enum ContractType { CDI, CDD, INTERIM, INTERNSHIP, FREELANCE }
    public enum EmployeeStatus { ACTIVE, ON_LEAVE, SUSPENDED, TERMINATED }
    public enum Country { FR, CI, SN, ML, BF, BE, OTHER }

    private final UUID id;
    private final TenantId tenantId;
    private final String employeeNumber;
    private EmployeeStatus status;

    // Identité
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String nationalId;       // SANS_CHIFFRE — chiffré en DB

    // Contrat
    private ContractType contractType;
    private LocalDate hireDate;
    private LocalDate contractEndDate;
    private String jobTitle;
    private String department;
    private String managerId;
    private Country country;

    // Rémunération
    private Money grossSalary;
    private Money netSalary;         // Calculé par le moteur de paie
    private String payFrequency;     // MONTHLY, WEEKLY

    // Congés (en jours)
    private int annualLeaveBalance;
    private int sickLeaveBalance;
    private int rttBalance;          // France uniquement

    // Données bancaires & administratives (chiffrées en base)
    private String socialSecurityNumber;
    private String bankIban;
    private String bankBic;

    // RGPD — Données personnelles
    // @PersonalData(category = DataCategory.IDENTITY, retention = "5_YEARS")

    private Employee(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.tenantId = builder.tenantId;
        this.employeeNumber = builder.employeeNumber;
        this.status = EmployeeStatus.ACTIVE;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.email = builder.email;
        this.phone = builder.phone;
        this.birthDate = builder.birthDate;
        this.nationalId = builder.nationalId;
        this.contractType = builder.contractType;
        this.hireDate = builder.hireDate;
        this.contractEndDate = builder.contractEndDate;
        this.jobTitle = builder.jobTitle;
        this.department = builder.department;
        this.managerId = builder.managerId;
        this.country = builder.country != null ? builder.country : Country.FR;
        this.grossSalary = builder.grossSalary;
        this.payFrequency = builder.payFrequency != null ? builder.payFrequency : "MONTHLY";
        this.annualLeaveBalance = builder.annualLeaveBalance > 0 ? builder.annualLeaveBalance : 25;
        this.sickLeaveBalance = 0;
        this.rttBalance = builder.rttBalance;
        this.socialSecurityNumber = builder.socialSecurityNumber;
        this.bankIban = builder.bankIban;
        this.bankBic = builder.bankBic;
    }

    // ─── Méthodes domaine ───────────────────────────────────────────────────

    public void terminate(LocalDate terminationDate, String reason) {
        if (status == EmployeeStatus.TERMINATED) {
            throw DomainException.invalidState("Employee", status.name(), "ACTIVE or ON_LEAVE");
        }
        this.status = EmployeeStatus.TERMINATED;
        this.contractEndDate = terminationDate;
    }

    public void requestLeave(int days, String leaveType) {
        if ("ANNUAL".equals(leaveType)) {
            if (days > annualLeaveBalance) {
                throw DomainException.of("INSUFFICIENT_LEAVE",
                    "Insufficient annual leave balance: requested=" + days + ", available=" + annualLeaveBalance);
            }
        } else if ("RTT".equals(leaveType) && country == Country.FR) {
            if (days > rttBalance) {
                throw DomainException.of("INSUFFICIENT_RTT",
                    "Insufficient RTT balance: requested=" + days + ", available=" + rttBalance);
            }
        }
    }

    public void approveLeave(int days, String leaveType) {
        if ("ANNUAL".equals(leaveType)) {
            this.annualLeaveBalance -= days;
        } else if ("RTT".equals(leaveType)) {
            this.rttBalance -= days;
        }
    }

    public void updateSalary(Money newGrossSalary, String reason) {
        if (newGrossSalary == null || !newGrossSalary.isPositive()) {
            throw DomainException.of("INVALID_SALARY", "Salary must be positive");
        }
        this.grossSalary = newGrossSalary;
    }

    public boolean isContractExpiringSoon(int daysThreshold) {
        if (contractType == ContractType.CDI || contractEndDate == null) return false;
        return contractEndDate.isBefore(LocalDate.now().plusDays(daysThreshold));
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Getters
    public UUID getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public String getEmployeeNumber() { return employeeNumber; }
    public EmployeeStatus getStatus() { return status; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDate getBirthDate() { return birthDate; }
    public ContractType getContractType() { return contractType; }
    public LocalDate getHireDate() { return hireDate; }
    public LocalDate getContractEndDate() { return contractEndDate; }
    public String getJobTitle() { return jobTitle; }
    public String getDepartment() { return department; }
    public String getManagerId() { return managerId; }
    public Country getCountry() { return country; }
    public Money getGrossSalary() { return grossSalary; }
    public Money getNetSalary() { return netSalary; }
    public String getPayFrequency() { return payFrequency; }
    public int getAnnualLeaveBalance() { return annualLeaveBalance; }
    public int getSickLeaveBalance() { return sickLeaveBalance; }
    public int getRttBalance() { return rttBalance; }
    public String getNationalId() { return nationalId; }
    public String getSocialSecurityNumber() { return socialSecurityNumber; }
    public String getBankIban() { return bankIban; }
    public String getBankBic() { return bankBic; }
    public LocalDate getTerminationDate() { return contractEndDate; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private TenantId tenantId;
        private String employeeNumber;
        private String firstName, lastName, email, phone, nationalId;
        private String socialSecurityNumber, bankIban, bankBic;
        private LocalDate birthDate, hireDate, contractEndDate;
        private ContractType contractType = ContractType.CDI;
        private String jobTitle, department, managerId;
        private Country country;
        private Money grossSalary;
        private String payFrequency;
        private int annualLeaveBalance = 25;
        private int rttBalance = 0;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(String t) { this.tenantId = TenantId.of(t); return this; }
        public Builder employeeNumber(String n) { this.employeeNumber = n; return this; }
        public Builder firstName(String n) { this.firstName = n; return this; }
        public Builder lastName(String n) { this.lastName = n; return this; }
        public Builder email(String e) { this.email = e; return this; }
        public Builder phone(String p) { this.phone = p; return this; }
        public Builder nationalId(String n) { this.nationalId = n; return this; }
        public Builder birthDate(LocalDate d) { this.birthDate = d; return this; }
        public Builder hireDate(LocalDate d) { this.hireDate = d; return this; }
        public Builder contractEndDate(LocalDate d) { this.contractEndDate = d; return this; }
        public Builder contractType(ContractType t) { this.contractType = t; return this; }
        public Builder jobTitle(String t) { this.jobTitle = t; return this; }
        public Builder department(String d) { this.department = d; return this; }
        public Builder managerId(String m) { this.managerId = m; return this; }
        public Builder country(Country c) { this.country = c; return this; }
        public Builder grossSalary(Money s) { this.grossSalary = s; return this; }
        public Builder payFrequency(String f) { this.payFrequency = f; return this; }
        public Builder annualLeaveBalance(int b) { this.annualLeaveBalance = b; return this; }
        public Builder rttBalance(int b) { this.rttBalance = b; return this; }
        public Builder socialSecurityNumber(String s) { this.socialSecurityNumber = s; return this; }
        public Builder bankIban(String i) { this.bankIban = i; return this; }
        public Builder bankBic(String b) { this.bankBic = b; return this; }

        public Employee build() {
            if (tenantId == null) throw new IllegalStateException("tenantId required");
            if (employeeNumber == null) throw new IllegalStateException("employeeNumber required");
            if (firstName == null || lastName == null) throw new IllegalStateException("name required");
            if (hireDate == null) throw new IllegalStateException("hireDate required");
            return new Employee(this);
        }
    }
}
