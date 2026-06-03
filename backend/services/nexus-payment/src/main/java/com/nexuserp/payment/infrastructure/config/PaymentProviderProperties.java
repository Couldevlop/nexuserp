package com.nexuserp.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Propriétés des providers Mobile Money.
 * Bind sur le préfixe {@code nexus.payment}.
 *
 * Exemple :
 *   nexus.payment.providers.orange_money.webhook-secret=${ORANGE_WEBHOOK_SECRET:dev-...}
 *
 * A02 (Crypto Failures) : les secrets ne sont JAMAIS écrits en dur dans le code ;
 * ils proviennent exclusivement de la config / variables d'environnement / Vault.
 */
@ConfigurationProperties(prefix = "nexus.payment")
public record PaymentProviderProperties(
    String callbackBaseUrl,
    Map<String, ProviderConfig> providers
) {
    public record ProviderConfig(
        String webhookSecret,
        String apiBaseUrl,
        String apiKey,
        boolean enabled
    ) {}

    /**
     * Récupère la config d'un provider par son nom d'enum (clé insensible à la casse).
     */
    public ProviderConfig forProvider(String providerName) {
        if (providers == null) return null;
        ProviderConfig direct = providers.get(providerName);
        if (direct != null) return direct;
        return providers.get(providerName.toLowerCase());
    }
}
