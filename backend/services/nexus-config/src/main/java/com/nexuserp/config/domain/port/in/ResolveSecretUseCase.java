package com.nexuserp.config.domain.port.in;

import java.util.Optional;

/**
 * Port IN — résolution d'une valeur DÉCHIFFRÉE (usage interne service-à-service uniquement).
 *
 * OWASP A02/A01 : exposé exclusivement via l'endpoint interne protégé par le rôle SERVICE.
 * Retourne la valeur en clair (secret déchiffré, ou valeur non-secrète telle quelle).
 */
public interface ResolveSecretUseCase {

    /** @return la valeur en clair si le paramètre existe et est défini, sinon empty. */
    Optional<String> resolve(String tenantId, String key);
}
