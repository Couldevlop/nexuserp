package com.nexuserp.payment.infrastructure.config;

import com.nexuserp.core.config.ConfigClient;
import com.nexuserp.core.infrastructure.tenant.TenantContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Résout la configuration EFFECTIVE d'un provider Mobile Money :
 * le store central nexus-config (par tenant, chiffré AES-256-GCM, géré depuis
 * l'UI admin Paramétrage) est PRIORITAIRE ; les variables d'environnement
 * ({@link PaymentProviderProperties}) restent le FALLBACK.
 *
 * Clés du store — alignées sur le catalogue UI admin (config-catalog.ts) :
 *   payment.orange.client-id / client-secret / merchant-key
 *   payment.wave.api-key
 *   payment.mtn.api-user / api-key / subscription-key
 *   payment.moov.base-url / api-key
 *   + par convention kebab-case : payment.&lt;provider&gt;.webhook-secret / base-url /
 *     token-url / return-url / cancel-url / currency / target-environment
 *
 * ACTIVATION ZÉRO-CODE (complément de PaymentProviderProperties) :
 * ajouter les clés requises dans l'UI admin suffit à basculer le provider de
 * SIMULÉ à RÉEL à la prochaine requête (cache ConfigClient ~60s) — sans variable
 * d'environnement NI redémarrage.
 *
 * Cas dégradés (non-bloquants, OWASP A04 fail-safe) :
 *  - ConfigClient absent/désactivé (nexus.config.url vide) -> env seul ;
 *  - pas de TenantContext (ex. webhooks entrants non authentifiés) -> env seul ;
 *  - nexus-config injoignable -> env seul (le ConfigClient avale les erreurs).
 */
@Component
public class ProviderConfigResolver {

    /** Préfixe de clé catalogue par provider (PaymentProvider.name() -> segment). */
    private static final Map<String, String> KEY_PREFIXES = Map.of(
        "ORANGE_MONEY", "orange",
        "WAVE", "wave",
        "MTN_MOMO", "mtn",
        "MOOV_MONEY", "moov");

    private final PaymentProviderProperties properties;
    @Nullable
    private final ConfigClient configClient;

    public ProviderConfigResolver(PaymentProviderProperties properties,
                                  @Nullable ConfigClient configClient) {
        this.properties = properties;
        this.configClient = configClient;
    }

    public String callbackBaseUrl() {
        return properties.callbackBaseUrl();
    }

    /**
     * Config effective du provider : chaque champ vaut la valeur du store si
     * définie (non vide), sinon la valeur issue de l'environnement.
     */
    public PaymentProviderProperties.ProviderConfig forProvider(String providerName) {
        PaymentProviderProperties.ProviderConfig env = properties.forProvider(providerName);
        String prefix = providerName != null ? KEY_PREFIXES.get(providerName.toUpperCase()) : null;
        // OrNull : les webhooks entrants n'ont pas de TenantContext -> fallback env.
        String tenantId = TenantContext.getTenantIdOrNull();
        if (configClient == null || prefix == null || tenantId == null) {
            return env;
        }

        PaymentProviderProperties.RealApiConfig r = env != null ? env.real() : null;
        // MTN MoMo : la clé catalogue "payment.mtn.api-key" désigne l'API User Key
        // (champ apiUserKey), pas le champ générique apiKey (inutilisé pour MTN).
        boolean mtn = "MTN_MOMO".equalsIgnoreCase(providerName);

        PaymentProviderProperties.RealApiConfig real = new PaymentProviderProperties.RealApiConfig(
            resolve(tenantId, prefix, "base-url", r != null ? r.baseUrl() : null),
            resolve(tenantId, prefix, "token-url", r != null ? r.tokenUrl() : null),
            resolve(tenantId, prefix, "client-id", r != null ? r.clientId() : null),
            resolve(tenantId, prefix, "client-secret", r != null ? r.clientSecret() : null),
            resolve(tenantId, prefix, "merchant-key", r != null ? r.merchantKey() : null),
            mtn ? (r != null ? r.apiKey() : null)
                : resolve(tenantId, prefix, "api-key", r != null ? r.apiKey() : null),
            resolve(tenantId, prefix, "api-user", r != null ? r.apiUser() : null),
            mtn ? resolve(tenantId, prefix, "api-key", r != null ? r.apiUserKey() : null)
                : (r != null ? r.apiUserKey() : null),
            resolve(tenantId, prefix, "subscription-key", r != null ? r.subscriptionKey() : null),
            resolve(tenantId, prefix, "target-environment", r != null ? r.targetEnvironment() : null),
            resolve(tenantId, prefix, "return-url", r != null ? r.returnUrl() : null),
            resolve(tenantId, prefix, "cancel-url", r != null ? r.cancelUrl() : null),
            resolve(tenantId, prefix, "currency", r != null ? r.currency() : null));

        return new PaymentProviderProperties.ProviderConfig(
            resolve(tenantId, prefix, "webhook-secret", env != null ? env.webhookSecret() : null),
            env != null ? env.apiBaseUrl() : null,
            env != null ? env.apiKey() : null,
            env == null || env.enabled(),
            real);
    }

    private String resolve(String tenantId, String prefix, String shortKey, String envValue) {
        return configClient.getValue(tenantId, "payment." + prefix + "." + shortKey)
            .filter(v -> !v.isBlank())
            .orElse(envValue);
    }
}
