package com.nexuserp.config.adapter.in.rest;

/**
 * Réponse de l'endpoint interne : valeur DÉCHIFFRÉE pour usage service-à-service.
 * Ne JAMAIS exposer via l'API admin.
 */
public record ResolvedValueDto(
    String key,
    String value
) {}
