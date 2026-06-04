package com.nexuserp.core.config;

import java.util.Optional;

/**
 * Client partagé de résolution de configuration centralisée (nexus-config).
 *
 * Permet à n'importe quel service NexusERP de lire ses paramètres/clés API depuis le
 * magasin chiffré central, sans dupliquer la logique : "ajouter la clé dans l'UI admin
 * -> elle s'active", sans changement de code côté consommateur.
 *
 * Contrat :
 *  - {@link #getValue(String, String)} interroge l'endpoint interne de nexus-config
 *    (GET /api/v1/config/internal/{key}/value) et renvoie la valeur DÉCHIFFRÉE.
 *  - Un cache mémoire court (par tenant+clé) évite un appel réseau à chaque lecture.
 *  - {@link #invalidate(String, String)} / {@link #invalidateTenant(String)} permettent
 *    d'évincer le cache (à brancher sur l'événement Kafka nexus.config.changed).
 *  - Si l'URL de nexus-config n'est pas configurée (prop {@code nexus.config.url}),
 *    l'implémentation par défaut renvoie {@link Optional#empty()} sans erreur :
 *    le consommateur retombe alors sur ses variables d'environnement (non-bloquant).
 *
 * Authentification (service-à-service) : voir {@link DefaultConfigClient}. Pour l'instant,
 * un jeton de service statique configurable ({@code nexus.config.service-token}) est
 * propagé en Bearer. La propagation du JWT entrant pourra être ajoutée ultérieurement.
 */
public interface ConfigClient {

    /**
     * Résout la valeur déchiffrée d'un paramètre pour un tenant.
     *
     * @param tenantId tenant courant
     * @param key      clé du paramètre (ex. "payment.wave.apiKey")
     * @return la valeur en clair si trouvée et nexus-config est joignable, sinon empty.
     */
    Optional<String> getValue(String tenantId, String key);

    /** Évince l'entrée de cache (tenant+clé). */
    void invalidate(String tenantId, String key);

    /** Évince toutes les entrées de cache d'un tenant. */
    void invalidateTenant(String tenantId);
}
