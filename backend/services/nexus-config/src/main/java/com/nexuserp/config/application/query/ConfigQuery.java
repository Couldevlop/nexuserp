package com.nexuserp.config.application.query;

import com.nexuserp.config.domain.model.ConfigCategory;

/**
 * Requête de lecture de configuration. {@code category} optionnelle (null = toutes).
 */
public record ConfigQuery(
    String tenantId,
    ConfigCategory category
) {}
