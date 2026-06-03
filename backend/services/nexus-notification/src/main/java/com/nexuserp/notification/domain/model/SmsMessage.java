package com.nexuserp.notification.domain.model;

import java.util.Map;

/**
 * Message destiné aux canaux courts (SMS / WhatsApp).
 * Le contenu texte est résolu par {@code SmsTemplateResolver} à partir du type + locale + variables.
 *
 * @param recipientPhone numéro au format E.164 (ex: +2250700000000)
 */
public record SmsMessage(
    String tenantId,
    String recipientPhone,
    String recipientName,
    NotificationMessage.NotificationType type,
    String locale,
    Map<String, Object> variables
) {
    /** Masque le numéro pour le logging (OWASP A09 — pas de PII en clair). */
    public String maskedPhone() {
        return PhoneMasker.mask(recipientPhone);
    }
}
