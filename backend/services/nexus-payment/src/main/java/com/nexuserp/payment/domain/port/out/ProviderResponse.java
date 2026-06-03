package com.nexuserp.payment.domain.port.out;

/**
 * Réponse normalisée d'un provider après initiation d'une collecte.
 *
 * @param accepted          true si le provider a pris la demande en charge
 * @param providerRef       référence de transaction côté provider
 * @param redirectUrl       URL de paiement (Wave, redirection web) — peut être null
 * @param ussdInstructions  instructions USSD/push (Orange/MTN/Moov) — peut être null
 * @param rawMessage        message brut diagnostique
 */
public record ProviderResponse(
    boolean accepted,
    String providerRef,
    String redirectUrl,
    String ussdInstructions,
    String rawMessage
) {
    public static ProviderResponse accepted(String providerRef, String redirectUrl, String ussd) {
        return new ProviderResponse(true, providerRef, redirectUrl, ussd, "ACCEPTED");
    }

    public static ProviderResponse rejected(String reason) {
        return new ProviderResponse(false, null, null, null, reason);
    }
}
