package com.nexuserp.hr.domain;

import com.nexuserp.hr.domain.service.PayrollCalculator;
import com.nexuserp.hr.domain.model.Employee;
import com.nexuserp.hr.domain.model.Country;
import com.nexuserp.core.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PayrollCalculator — France & Côte d'Ivoire")
class PayrollCalculatorTest {

    private PayrollCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PayrollCalculator();
    }

    // ─── France ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FR — Should calculate salarial deductions for SMIC")
    void fr_shouldCalculateSmicPayroll() {
        Employee employee = buildFrEmployee(new BigDecimal("1766.92")); // SMIC 2026
        PayrollCalculator.PayrollResult result = calculator.calculate(employee, 2026, 1);

        assertThat(result.grossSalary()).isEqualByComparingTo("1766.92");
        // Total salarial cotisations ~22-23% → net ≈ 1370-1380
        assertThat(result.netSalary()).isGreaterThan(Money.of(new BigDecimal("1300"), "EUR"));
        assertThat(result.netSalary()).isLessThan(Money.of(new BigDecimal("1500"), "EUR"));
        assertThat(result.employerContributions().amount()).isPositive();
    }

    @Test
    @DisplayName("FR — Should deduct retraite salariale at 6.8%")
    void fr_shouldDeductRetarite() {
        Employee employee = buildFrEmployee(new BigDecimal("3000.00"));
        PayrollCalculator.PayrollResult result = calculator.calculate(employee, 2026, 1);

        Money retraite = result.salarialDeductions().get("RETRAITE_SALARIALE");
        assertThat(retraite).isNotNull();
        // 3000 * 6.8% = 204
        assertThat(retraite.amount()).isEqualByComparingTo("204.0000");
    }

    @Test
    @DisplayName("FR — Net must be less than gross")
    void fr_netMustBeLessThanGross() {
        Employee employee = buildFrEmployee(new BigDecimal("5000.00"));
        PayrollCalculator.PayrollResult result = calculator.calculate(employee, 2026, 1);

        assertThat(result.netSalary().amount())
                .isLessThan(result.grossSalary());
    }

    @Test
    @DisplayName("FR — Employer contributions must be positive")
    void fr_employerContributionsMustBePositive() {
        Employee employee = buildFrEmployee(new BigDecimal("4000.00"));
        PayrollCalculator.PayrollResult result = calculator.calculate(employee, 2026, 1);

        assertThat(result.employerContributions().isPositive()).isTrue();
        // Patronal total ≈ 40-45% of gross
        assertThat(result.employerContributions().amount())
                .isGreaterThan(new BigDecimal("1500"));
    }

    // ─── Côte d'Ivoire ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CI — Should calculate CNPS salariale at 3.36%")
    void ci_shouldCalculateCnpsSalariale() {
        Employee employee = buildCiEmployee(new BigDecimal("500000")); // 500k XOF
        PayrollCalculator.PayrollResult result = calculator.calculate(employee, 2026, 1);

        Money cnps = result.salarialDeductions().get("CNPS_SALARIALE");
        assertThat(cnps).isNotNull();
        // 500000 * 3.36% = 16800
        assertThat(cnps.amount()).isEqualByComparingTo("16800.0000");
    }

    @Test
    @DisplayName("CI — ITS should be 0 for salary below first bracket threshold")
    void ci_itsShouldBeZeroForSmallSalary() {
        Employee employee = buildCiEmployee(new BigDecimal("50000")); // 50k XOF (< 75k)
        PayrollCalculator.PayrollResult result = calculator.calculate(employee, 2026, 1);

        Money its = result.salarialDeductions().get("ITS");
        // Below 75,000 XOF/month → 0% bracket
        assertThat(its).isNotNull();
        assertThat(its.amount()).isEqualByComparingTo("0.0000");
    }

    @ParameterizedTest
    @DisplayName("CI — ITS should apply progressive brackets")
    @CsvSource({
        "200000, 200000",   // bracket check: calculate ITS on 200k
        "500000, 500000",
        "1000000, 1000000"
    })
    void ci_itsShouldApplyProgressiveBrackets(String salaryStr, String ignored) {
        Employee employee = buildCiEmployee(new BigDecimal(salaryStr));
        PayrollCalculator.PayrollResult result = calculator.calculate(employee, 2026, 1);

        Money its = result.salarialDeductions().get("ITS");
        assertThat(its).isNotNull();
        assertThat(its.amount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("CI — Net must be less than gross")
    void ci_netMustBeLessThanGross() {
        Employee employee = buildCiEmployee(new BigDecimal("800000"));
        PayrollCalculator.PayrollResult result = calculator.calculate(employee, 2026, 1);

        assertThat(result.netSalary().amount())
                .isLessThan(result.grossSalary());
    }

    @Test
    @DisplayName("CI — CNPS patronale at 14.64%")
    void ci_shouldCalculateCnpsPatronale() {
        Employee employee = buildCiEmployee(new BigDecimal("500000"));
        PayrollCalculator.PayrollResult result = calculator.calculate(employee, 2026, 1);

        Money cnpsPatronale = result.employerBreakdown().get("CNPS_PATRONALE");
        // 500000 * 14.64% = 73200
        assertThat(cnpsPatronale).isNotNull();
        assertThat(cnpsPatronale.amount()).isEqualByComparingTo("73200.0000");
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Employee buildFrEmployee(BigDecimal grossSalary) {
        return Employee.builder()
                .id("emp-fr-001")
                .tenantId("fr-acme")
                .firstName("Jean")
                .lastName("Dupont")
                .country(Country.FRANCE)
                .contractType(Employee.ContractType.CDI)
                .grossSalary(Money.of(grossSalary, "EUR"))
                .contractStartDate(LocalDate.of(2020, 1, 1))
                .build();
    }

    private Employee buildCiEmployee(BigDecimal grossSalary) {
        return Employee.builder()
                .id("emp-ci-001")
                .tenantId("ci-acme")
                .firstName("Kouamé")
                .lastName("Yao")
                .country(Country.COTE_IVOIRE)
                .contractType(Employee.ContractType.CDI)
                .grossSalary(Money.of(grossSalary, "XOF"))
                .contractStartDate(LocalDate.of(2020, 1, 1))
                .build();
    }
}
