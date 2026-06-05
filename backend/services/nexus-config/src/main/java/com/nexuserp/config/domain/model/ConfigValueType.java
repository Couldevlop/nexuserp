package com.nexuserp.config.domain.model;

import com.nexuserp.core.domain.exception.DomainException;

/**
 * Type de valeur d'un paramètre. SECRET implique un chiffrement obligatoire au repos
 * et un masquage systématique en lecture (jamais retourné en clair par l'API admin).
 */
public enum ConfigValueType {
    STRING,
    NUMBER,
    BOOLEAN,
    JSON,
    SECRET;

    /**
     * Validation légère de cohérence valeur/type (A03 — validation des entrées).
     * Ne lève pas pour SECRET/JSON/STRING (format libre) ; vérifie NUMBER et BOOLEAN.
     */
    public void validate(String rawValue) {
        if (rawValue == null) {
            return;
        }
        switch (this) {
            case NUMBER -> {
                try {
                    Double.parseDouble(rawValue.trim());
                } catch (NumberFormatException e) {
                    throw DomainException.of("CONFIG_INVALID", "value is not a valid NUMBER: " + rawValue);
                }
            }
            case BOOLEAN -> {
                String v = rawValue.trim().toLowerCase();
                if (!v.equals("true") && !v.equals("false")) {
                    throw DomainException.of("CONFIG_INVALID", "value is not a valid BOOLEAN (true/false)");
                }
            }
            default -> { /* STRING, JSON, SECRET — format libre */ }
        }
    }
}
