package com.nexuserp.config.adapter.in.rest;

import com.nexuserp.config.domain.model.ConfigCategory;
import com.nexuserp.config.domain.model.ConfigValueType;
import com.nexuserp.config.domain.model.ConfigView;

import java.time.Instant;

/**
 * DTO de réponse pour l'API admin. Représentation SÛRE :
 *  - pour un secret, {@code value} vaut le masque "••••••" (ou null si non défini)
 *    et {@code set} indique si une valeur existe ; le clair n'est jamais exposé.
 */
public record ConfigDto(
    String key,
    ConfigCategory category,
    ConfigValueType type,
    boolean secret,
    String value,
    boolean set,
    String description,
    String updatedBy,
    Instant updatedAt
) {
    public static ConfigDto from(ConfigView v) {
        return new ConfigDto(
            v.key(), v.category(), v.valueType(), v.secret(),
            v.value(), v.set(), v.description(), v.updatedBy(), v.updatedAt());
    }
}
