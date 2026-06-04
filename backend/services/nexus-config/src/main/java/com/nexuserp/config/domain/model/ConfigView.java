package com.nexuserp.config.domain.model;

import java.time.Instant;

/**
 * Vue de lecture d'un paramètre, SÛRE pour exposition via l'API admin.
 *
 * Pour un secret : {@code value} est masqué ("••••••") et {@code set} indique si une
 * valeur est définie. La valeur en clair n'apparaît JAMAIS ici (A02/A09).
 * Pour un non-secret : {@code value} contient la valeur réelle.
 */
public record ConfigView(
    String key,
    ConfigCategory category,
    ConfigValueType valueType,
    boolean secret,
    String value,
    boolean set,
    String description,
    String updatedBy,
    Instant updatedAt
) {
    public static final String MASK = "••••••";

    /** Construit une vue masquée à partir d'un agrégat (secrets jamais déchiffrés). */
    public static ConfigView masked(ConfigParameter p) {
        boolean isSecret = p.isSecret();
        String shownValue = isSecret ? (p.hasValue() ? MASK : null) : p.getStoredValue();
        return new ConfigView(
            p.getKey(), p.getCategory(), p.getValueType(), isSecret,
            shownValue, p.hasValue(), p.getDescription(), p.getUpdatedBy(), p.getUpdatedAt());
    }
}
