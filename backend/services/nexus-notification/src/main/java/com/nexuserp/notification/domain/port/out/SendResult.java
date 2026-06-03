package com.nexuserp.notification.domain.port.out;

/**
 * Résultat normalisé d'un envoi via un fournisseur externe (SMS / WhatsApp).
 *
 * @param providerMessageId identifiant retourné par le fournisseur (null si refusé)
 * @param accepted          true si le fournisseur a accepté le message
 * @param error             message d'erreur (null si succès)
 */
public record SendResult(
    String providerMessageId,
    boolean accepted,
    String error
) {
    public static SendResult accepted(String providerMessageId) {
        return new SendResult(providerMessageId, true, null);
    }

    public static SendResult failed(String error) {
        return new SendResult(null, false, error);
    }
}
