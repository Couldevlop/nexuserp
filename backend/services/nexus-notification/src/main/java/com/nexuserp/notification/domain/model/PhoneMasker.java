package com.nexuserp.notification.domain.model;

/**
 * Masquage des numéros de téléphone pour le logging (OWASP A09 :
 * ne jamais journaliser un numéro complet — donnée personnelle).
 * Conserve l'indicatif et les 2 derniers chiffres : +225******89.
 */
public final class PhoneMasker {

    private PhoneMasker() {}

    public static String mask(String phone) {
        if (phone == null || phone.isBlank()) {
            return "<none>";
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 5) {
            return "***";
        }
        // indicatif = +XXX (jusqu'à 4 premiers caractères), suffixe = 2 derniers
        int prefixLen = Math.min(4, trimmed.length() - 2);
        String prefix = trimmed.substring(0, prefixLen);
        String suffix = trimmed.substring(trimmed.length() - 2);
        int hiddenLen = trimmed.length() - prefixLen - 2;
        return prefix + "*".repeat(Math.max(hiddenLen, 1)) + suffix;
    }
}
