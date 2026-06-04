package com.nexuserp.config.adapter.in.rest;

import com.nexuserp.config.domain.model.ConfigCategory;
import com.nexuserp.config.domain.model.ConfigValueType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de la requête d'upsert d'un paramètre (PUT /api/v1/config/{key}).
 *
 * A03 (Injection) : entrées validées par Jakarta Validation.
 * {@code value} est la valeur EN CLAIR ; elle est chiffrée côté serveur si secret.
 */
public record UpsertConfigRequest(
    @Size(max = 16384) String value,
    @NotNull ConfigValueType type,
    @NotNull ConfigCategory category,
    boolean secret,
    @Size(max = 1000) String description
) {}
