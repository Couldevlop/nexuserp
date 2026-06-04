package com.nexuserp.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implémentation par défaut de {@link ConfigClient} basée sur Spring {@link RestClient}.
 *
 * Conçue pour être OPTIONNELLE et NON-BLOQUANTE :
 *  - si {@code nexus.config.url} est vide, tous les appels renvoient empty (no-op) ;
 *  - toute erreur réseau/HTTP est avalée et renvoie empty (le consommateur retombe sur l'env).
 *
 * Cache mémoire : TTL court ({@code nexus.config.cache-ttl-seconds}, défaut 60s) par tenant+clé,
 * invalidable à la réception de l'événement Kafka nexus.config.changed.
 *
 * Cette implémentation est volontairement légère ; un service qui n'a pas besoin du
 * client central peut simplement ne jamais l'injecter.
 */
@Component
public class DefaultConfigClient implements ConfigClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultConfigClient.class);

    private final String baseUrl;
    private final String serviceToken;
    private final Duration cacheTtl;
    private final RestClient restClient;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public DefaultConfigClient(
            @Value("${nexus.config.url:}") String baseUrl,
            @Value("${nexus.config.service-token:}") String serviceToken,
            @Value("${nexus.config.cache-ttl-seconds:60}") long cacheTtlSeconds) {
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "";
        this.serviceToken = serviceToken != null ? serviceToken.trim() : "";
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
        this.restClient = this.baseUrl.isEmpty() ? null : RestClient.builder().baseUrl(this.baseUrl).build();
        if (this.baseUrl.isEmpty()) {
            log.info("ConfigClient disabled (nexus.config.url not set) — callers fall back to environment variables.");
        }
    }

    @Override
    public Optional<String> getValue(String tenantId, String key) {
        if (restClient == null || tenantId == null || key == null) {
            return Optional.empty();
        }
        String cacheKey = tenantId + "::" + key;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return Optional.ofNullable(cached.value());
        }

        try {
            ResolvedValue body = restClient.get()
                .uri("/api/v1/config/internal/{key}/value", key)
                .headers(h -> {
                    // X-Tenant-Id : consommé par le TenantInterceptor de nexus-config.
                    h.set("X-Tenant-Id", tenantId);
                    if (!serviceToken.isEmpty()) {
                        h.setBearerAuth(serviceToken);
                    }
                })
                .retrieve()
                .body(ResolvedValue.class);

            String value = body != null ? body.value() : null;
            cache.put(cacheKey, new CacheEntry(value, Instant.now().plus(cacheTtl)));
            return Optional.ofNullable(value);
        } catch (HttpClientErrorException.NotFound e) {
            // 404 = paramètre non défini dans le store : cache NÉGATIF (évite un
            // appel HTTP par lecture pour les clés absentes — cas majoritaire).
            cache.put(cacheKey, new CacheEntry(null, Instant.now().plus(cacheTtl)));
            return Optional.empty();
        } catch (Exception e) {
            // Non-bloquant : journaliser sans la valeur, retomber sur l'env côté appelant.
            // Erreur réseau/transitoire : PAS de cache négatif (retente au prochain appel).
            log.debug("ConfigClient could not resolve key={} for tenant={}: {}", key, tenantId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void invalidate(String tenantId, String key) {
        if (tenantId != null && key != null) {
            cache.remove(tenantId + "::" + key);
        }
    }

    @Override
    public void invalidateTenant(String tenantId) {
        if (tenantId == null) {
            return;
        }
        String prefix = tenantId + "::";
        cache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /** Réponse de l'endpoint interne de nexus-config. */
    private record ResolvedValue(String key, String value) {}

    private record CacheEntry(String value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
