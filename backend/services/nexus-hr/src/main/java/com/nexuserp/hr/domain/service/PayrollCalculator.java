package com.nexuserp.hr.domain.service;

import com.nexuserp.core.domain.value.Money;
import com.nexuserp.hr.domain.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Moteur de calcul paie — France & Côte d'Ivoire.
 * France : URSSAF, AGIRC-ARRCO, CSG-CRDS, Net imposable.
 * CI : CNPS, ITS, Taxe d'apprentissage, FDFP.
 */
@Component
public class PayrollCalculator {

    // ─── France — Taux cotisations 2026 ─────────────────────────────────────
    private static final BigDecimal FR_SANTE_SALARIALE        = new BigDecimal("0.0000");  // Exonéré depuis 2018
    private static final BigDecimal FR_RETRAITE_SALARIALE     = new BigDecimal("0.0680");
    private static final BigDecimal FR_RETRAITE_COMPLEMENTAIRE= new BigDecimal("0.0315");
    private static final BigDecimal FR_CHOMAGE_SALARIALE      = new BigDecimal("0.0000");  // Exonéré salarié
    private static final BigDecimal FR_CSG_CRDS_NON_IMPO      = new BigDecimal("0.0298");
    private static final BigDecimal FR_CSG_DEDUCTIBLE         = new BigDecimal("0.0688");

    // Employeur
    private static final BigDecimal FR_SANTE_PATRONALE        = new BigDecimal("0.1300");
    private static final BigDecimal FR_RETRAITE_PATRONALE     = new BigDecimal("0.0860");
    private static final BigDecimal FR_RETRAITE_COMP_PATRONALE= new BigDecimal("0.0210");
    private static final BigDecimal FR_CHOMAGE_PATRONALE      = new BigDecimal("0.0405");
    private static final BigDecimal FR_ACCIDENTS_TRAVAIL      = new BigDecimal("0.0200"); // Variable

    // ─── CI — Taux cotisations ──────────────────────────────────────────────
    private static final BigDecimal CI_CNPS_SALARIALE         = new BigDecimal("0.0336"); // 3.36%
    private static final BigDecimal CI_CNPS_PATRONALE         = new BigDecimal("0.1464"); // 14.64%
    private static final BigDecimal CI_TAXE_APPRENTISSAGE     = new BigDecimal("0.0040"); // 0.4% patronal
    private static final BigDecimal CI_FDFP_PATRONAL          = new BigDecimal("0.0160"); // 1.6% patronal

    public PayslipResult calculate(Employee employee) {
        return switch (employee.getCountry()) {
            case FR -> calculateFrance(employee);
            case CI, SN, ML, BF -> calculateUEMOA(employee);
            default -> calculateFrance(employee);
        };
    }

    private PayslipResult calculateFrance(Employee employee) {
        BigDecimal grossAmount = employee.getGrossSalary().amount();
        String currency = employee.getGrossSalary().currency();

        // Cotisations salariales
        BigDecimal retraiteSalariale = grossAmount.multiply(FR_RETRAITE_SALARIALE);
        BigDecimal retraiteComp = grossAmount.multiply(FR_RETRAITE_COMPLEMENTAIRE);
        BigDecimal csgCrdsNonImpo = grossAmount.multiply(new BigDecimal("0.9825")).multiply(FR_CSG_CRDS_NON_IMPO);
        BigDecimal csgDeductible = grossAmount.multiply(new BigDecimal("0.9825")).multiply(FR_CSG_DEDUCTIBLE);

        BigDecimal totalSalarialDeductions = retraiteSalariale.add(retraiteComp)
            .add(csgCrdsNonImpo).add(csgDeductible);

        BigDecimal netAmount = grossAmount.subtract(totalSalarialDeductions)
            .setScale(2, RoundingMode.HALF_UP);

        // Cotisations patronales
        BigDecimal santePatronale = grossAmount.multiply(FR_SANTE_PATRONALE);
        BigDecimal retraitePatronale = grossAmount.multiply(FR_RETRAITE_PATRONALE);
        BigDecimal retraiteCompPatronale = grossAmount.multiply(FR_RETRAITE_COMP_PATRONALE);
        BigDecimal chomagePatronale = grossAmount.multiply(FR_CHOMAGE_PATRONALE);
        BigDecimal accidentsPatronale = grossAmount.multiply(FR_ACCIDENTS_TRAVAIL);

        BigDecimal totalEmployerCost = grossAmount
            .add(santePatronale).add(retraitePatronale).add(retraiteCompPatronale)
            .add(chomagePatronale).add(accidentsPatronale)
            .setScale(2, RoundingMode.HALF_UP);

        return new PayslipResult(
            Money.of(grossAmount, currency),
            Money.of(netAmount, currency),
            Money.of(totalSalarialDeductions, currency),
            Money.of(totalEmployerCost, currency),
            "FR"
        );
    }

    private PayslipResult calculateUEMOA(Employee employee) {
        BigDecimal grossAmount = employee.getGrossSalary().amount();
        String currency = employee.getGrossSalary().currency();

        // CNPS salarié
        BigDecimal cnpsSalariale = grossAmount.multiply(CI_CNPS_SALARIALE);

        // ITS (Impôt sur Traitement et Salaires) — Barème progressif CI
        BigDecimal its = calculateITS(grossAmount);

        BigDecimal totalSalarialDeductions = cnpsSalariale.add(its)
            .setScale(0, RoundingMode.HALF_UP); // XOF = entiers

        BigDecimal netAmount = grossAmount.subtract(totalSalarialDeductions)
            .setScale(0, RoundingMode.HALF_UP);

        // Charges patronales
        BigDecimal cnpsPatronale = grossAmount.multiply(CI_CNPS_PATRONALE);
        BigDecimal taxeApprentissage = grossAmount.multiply(CI_TAXE_APPRENTISSAGE);
        BigDecimal fdfp = grossAmount.multiply(CI_FDFP_PATRONAL);

        BigDecimal totalEmployerCost = grossAmount.add(cnpsPatronale)
            .add(taxeApprentissage).add(fdfp)
            .setScale(0, RoundingMode.HALF_UP);

        return new PayslipResult(
            Money.of(grossAmount, currency),
            Money.of(netAmount, currency),
            Money.of(totalSalarialDeductions, currency),
            Money.of(totalEmployerCost, currency),
            "CI"
        );
    }

    /**
     * Barème ITS Côte d'Ivoire (progressif).
     */
    private BigDecimal calculateITS(BigDecimal grossMonthly) {
        // Barème mensuel simplifié CI 2026
        double gross = grossMonthly.doubleValue();
        double its;
        if (gross <= 75000) {
            its = 0;
        } else if (gross <= 150000) {
            its = (gross - 75000) * 0.02;
        } else if (gross <= 350000) {
            its = 1500 + (gross - 150000) * 0.10;
        } else if (gross <= 600000) {
            its = 21500 + (gross - 350000) * 0.20;
        } else if (gross <= 1500000) {
            its = 71500 + (gross - 600000) * 0.30;
        } else {
            its = 341500 + (gross - 1500000) * 0.35;
        }
        return BigDecimal.valueOf(its).setScale(0, RoundingMode.HALF_UP);
    }

    public record PayslipResult(
        Money grossSalary,
        Money netSalary,
        Money totalEmployeeDeductions,
        Money totalEmployerCost,
        String country
    ) {}
}
