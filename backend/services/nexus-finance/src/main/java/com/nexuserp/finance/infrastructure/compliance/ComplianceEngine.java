package com.nexuserp.finance.infrastructure.compliance;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.finance.application.command.CreateInvoiceCommand;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Moteur de conformité légale multi-juridictionnel.
 * Applique les règles PCG (France), SYSCOHADA (CI/UEMOA), IFRS (autres).
 * Détecte automatiquement la juridiction via le tenantId country.
 */
@Component
public class ComplianceEngine {

    // Taux TVA par pays
    private static final Map<String, Set<BigDecimal>> VALID_TAX_RATES = Map.of(
        "FR", Set.of(new BigDecimal("0"), new BigDecimal("2.10"),
                     new BigDecimal("5.50"), new BigDecimal("10.00"), new BigDecimal("20.00")),
        "CI", Set.of(new BigDecimal("0"), new BigDecimal("18.00")),
        "SN", Set.of(new BigDecimal("0"), new BigDecimal("18.00")),
        "ML", Set.of(new BigDecimal("0"), new BigDecimal("18.00")),
        "BF", Set.of(new BigDecimal("0"), new BigDecimal("18.00")),
        "BE", Set.of(new BigDecimal("0"), new BigDecimal("6.00"),
                     new BigDecimal("12.00"), new BigDecimal("21.00"))
    );

    // Devises autorisées par pays
    private static final Map<String, Set<String>> ALLOWED_CURRENCIES = Map.of(
        "FR", Set.of("EUR"),
        "CI", Set.of("XOF", "EUR", "USD"),
        "SN", Set.of("XOF", "EUR", "USD"),
        "BE", Set.of("EUR")
    );

    /**
     * Valide la création d'une facture selon les règles du pays.
     */
    public void validateInvoiceCreation(CreateInvoiceCommand cmd) {
        // Récupérer le pays depuis le contexte tenant (simplifié ici — en prod: lookup DB)
        String country = resolveCountry(cmd.tenantId());

        validateCurrency(cmd.currency(), country);
        validateTaxRates(cmd, country);
        validateMandatoryFields(cmd, country);
    }

    private void validateCurrency(String currency, String country) {
        Set<String> allowed = ALLOWED_CURRENCIES.getOrDefault(country, Set.of("EUR", "USD", "GBP", "XOF"));
        if (currency != null && !allowed.contains(currency)) {
            throw DomainException.of("INVALID_CURRENCY",
                "Currency " + currency + " is not allowed for country " + country +
                ". Allowed: " + allowed);
        }
    }

    private void validateTaxRates(CreateInvoiceCommand cmd, String country) {
        if (cmd.lines() == null) return;
        Set<BigDecimal> validRates = VALID_TAX_RATES.getOrDefault(country, Set.of());
        if (validRates.isEmpty()) return; // IFRS — pas de restriction

        cmd.lines().forEach(line -> {
            if (line.taxRate() != null && !validRates.contains(line.taxRate())) {
                throw DomainException.of("INVALID_TAX_RATE",
                    "Tax rate " + line.taxRate() + "% is not valid for country " + country +
                    ". Valid rates: " + validRates);
            }
        });
    }

    private void validateMandatoryFields(CreateInvoiceCommand cmd, String country) {
        // France : numéro TVA obligatoire pour les factures > 150€ B2B
        if ("FR".equals(country) && "CUSTOMER".equals(cmd.invoiceType())) {
            // Vérification simplifiée — en prod: vérification via VIES
            if (cmd.partnerName() == null || cmd.partnerName().isBlank()) {
                throw DomainException.of("MISSING_PARTNER_NAME", "Partner name is required for French invoices");
            }
        }
    }

    /**
     * Résout le pays d'un tenant.
     * En production : lookup dans tenant_management.tenants.
     * Ici simplifié : extraction depuis le tenantId (ex: fr-acme-corp → FR).
     */
    private String resolveCountry(String tenantId) {
        if (tenantId == null) return "FR";
        String lower = tenantId.toLowerCase();
        if (lower.startsWith("fr-") || lower.endsWith("-fr")) return "FR";
        if (lower.startsWith("ci-") || lower.endsWith("-ci")) return "CI";
        if (lower.startsWith("sn-") || lower.endsWith("-sn")) return "SN";
        if (lower.startsWith("be-") || lower.endsWith("-be")) return "BE";
        return "FR"; // Défaut France
    }
}
