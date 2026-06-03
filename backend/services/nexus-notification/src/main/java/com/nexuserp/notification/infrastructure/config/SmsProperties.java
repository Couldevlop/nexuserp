package com.nexuserp.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration des passerelles SMS / WhatsApp.
 * Toutes les valeurs sensibles proviennent de l'environnement (jamais de littéraux).
 * Si les credentials sont vides -> bascule automatique en mode simulé (dev).
 */
@ConfigurationProperties(prefix = "nexus.notification")
public record SmsProperties(
    Sms sms,
    WhatsApp whatsapp
) {

    public SmsProperties {
        if (sms == null) sms = new Sms(null, null, null, null);
        if (whatsapp == null) whatsapp = new WhatsApp(null, null, null);
    }

    /**
     * @param provider agrégateur (ex: orange, generic) — informatif
     * @param baseUrl  URL de base de l'API d'envoi
     * @param apiKey   clé API (secret, via env)
     * @param senderId identifiant expéditeur affiché (alphanumeric sender ID)
     */
    public record Sms(String provider, String baseUrl, String apiKey, String senderId) {
        public boolean hasCredentials() {
            return notBlank(baseUrl) && notBlank(apiKey);
        }
    }

    /**
     * WhatsApp Business Cloud API (Meta).
     * @param baseUrl     base Graph API (ex: https://graph.facebook.com/v20.0)
     * @param apiKey      bearer token (secret, via env)
     * @param fromPhoneId identifiant du numéro expéditeur (phone_number_id)
     */
    public record WhatsApp(String baseUrl, String apiKey, String fromPhoneId) {
        public boolean hasCredentials() {
            return notBlank(baseUrl) && notBlank(apiKey) && notBlank(fromPhoneId);
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
