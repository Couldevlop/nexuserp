package com.nexuserp.payment.domain.model;

import com.nexuserp.core.domain.exception.DomainException;

import java.util.regex.Pattern;

/**
 * Value Object — compte Mobile Money identifié par son MSISDN (numéro de téléphone).
 *
 * Validation E.164 (focalisée Côte d'Ivoire / UEMOA) dans le constructeur :
 *  - format international +225XXXXXXXXXX
 *  - ou format national 10 chiffres (préfixé automatiquement +225)
 *
 * A03 (Injection) : le MSISDN est strictement validé avant tout usage downstream.
 * A09 (Logging) : exposer uniquement masked() dans les logs, jamais value().
 */
public record MobileMoneyAccount(String value) {

    // E.164 : '+', indicatif pays 1-3 chiffres, puis chiffres significatifs (total <= 15).
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final String CI_PREFIX = "+225";

    public MobileMoneyAccount {
        value = normalize(value);
        if (!E164.matcher(value).matches()) {
            // Pas de leak du numéro complet dans le message d'erreur côté logs.
            throw DomainException.of("INVALID_MSISDN",
                "Invalid mobile money MSISDN (expected E.164, e.g. +2250700000000)");
        }
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw DomainException.of("INVALID_MSISDN", "MSISDN cannot be null or blank");
        }
        String cleaned = raw.replaceAll("[\\s\\-().]", "");
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
        }
        // Numéro national ivoirien (10 chiffres) -> préfixe +225.
        if (!cleaned.startsWith("+") && cleaned.matches("\\d{10}")) {
            cleaned = CI_PREFIX + cleaned;
        }
        return cleaned;
    }

    public static MobileMoneyAccount of(String value) {
        return new MobileMoneyAccount(value);
    }

    /**
     * Représentation masquée pour les logs/affichage (A09 — Logging Failures).
     * Conserve l'indicatif et les 2 derniers chiffres : +225******89.
     */
    public String masked() {
        int keepStart = Math.min(4, value.length());
        int keepEnd = 2;
        if (value.length() <= keepStart + keepEnd) {
            return "*".repeat(value.length());
        }
        String start = value.substring(0, keepStart);
        String end = value.substring(value.length() - keepEnd);
        return start + "*".repeat(value.length() - keepStart - keepEnd) + end;
    }

    @Override
    public String toString() {
        return masked();
    }
}
