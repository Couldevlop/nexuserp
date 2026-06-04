package com.nexuserp.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Propriétés des providers Mobile Money.
 * Bind sur le préfixe {@code nexus.payment}.
 *
 * Exemple :
 *   nexus.payment.providers.orange_money.webhook-secret=${ORANGE_WEBHOOK_SECRET:dev-...}
 *   nexus.payment.providers.orange_money.real.client-id=${ORANGE_CLIENT_ID:}
 *
 * A02 (Crypto Failures) : les secrets ne sont JAMAIS écrits en dur dans le code ;
 * ils proviennent exclusivement de la config / variables d'environnement / Vault.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * ACTIVATION ZÉRO-CODE (réel vs simulé) :
 * Chaque provider possède un bloc {@code real} (cf. {@link RealApiConfig}). Tant que
 * les identifiants requis de ce bloc sont vides (défaut), la stratégie SIMULÉE est
 * utilisée. Dès que les variables d'environnement correspondantes sont renseignées,
 * {@code real().isConfigured()} bascule à {@code true} et la stratégie RÉELLE prend
 * automatiquement le relais — sans aucune modification de code.
 * ──────────────────────────────────────────────────────────────────────────────
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
        boolean enabled,
        RealApiConfig real
    ) {
        /** True si le bloc {@code real} contient des identifiants exploitables pour ce provider. */
        public boolean isRealConfigured(String providerName) {
            return real != null && real.isConfigured(providerName);
        }
    }

    /**
     * Configuration des API RÉELLES des providers. Tous les champs sont lus depuis
     * l'environnement avec une valeur par défaut VIDE : tant qu'ils restent vides,
     * la stratégie simulée reste active.
     *
     * Champs (mutualisés ; chaque provider n'en utilise qu'un sous-ensemble) :
     *  - baseUrl                 : racine de l'API réelle (override de apiBaseUrl)
     *  - tokenUrl                : endpoint OAuth2 token (Orange Money)
     *  - clientId / clientSecret : OAuth2 client-credentials (Orange Money)
     *  - merchantKey             : clé marchand / merchant code (Orange Money WebPay)
     *  - apiKey                  : Bearer API key (Wave, Moov)
     *  - apiUser / apiUserKey    : API User + API Key (MTN MoMo Collection)
     *  - subscriptionKey         : Ocp-Apim-Subscription-Key (MTN MoMo)
     *  - targetEnvironment       : X-Target-Environment (sandbox|mtncongo|... MTN MoMo)
     *  - returnUrl / cancelUrl   : URLs de retour navigateur (Orange WebPay, Wave)
     *  - currency                : devise par défaut envoyée au provider
     */
    public record RealApiConfig(
        String baseUrl,
        String tokenUrl,
        String clientId,
        String clientSecret,
        String merchantKey,
        String apiKey,
        String apiUser,
        String apiUserKey,
        String subscriptionKey,
        String targetEnvironment,
        String returnUrl,
        String cancelUrl,
        String currency
    ) {
        private static boolean present(String v) {
            return v != null && !v.isBlank();
        }

        /**
         * Détermine si les identifiants REQUIS du provider sont présents.
         * La logique est spécifique car chaque provider a un schéma d'auth différent.
         */
        public boolean isConfigured(String providerName) {
            if (providerName == null) return false;
            return switch (providerName.toUpperCase()) {
                // Orange Money WebPay : OAuth2 client-credentials + clé marchand.
                case "ORANGE_MONEY" -> present(clientId) && present(clientSecret) && present(merchantKey);
                // Wave Checkout : une seule clé API Bearer suffit.
                case "WAVE" -> present(apiKey);
                // MTN MoMo Collection : API User + API Key + Subscription Key.
                case "MTN_MOMO" -> present(apiUser) && present(apiUserKey) && present(subscriptionKey);
                // Moov : adaptateur REST générique piloté par config -> baseUrl + apiKey.
                case "MOOV_MONEY" -> present(baseUrl) && present(apiKey);
                default -> false;
            };
        }
    }

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
