package com.nexuserp.payment.domain.port.in;

import com.nexuserp.payment.domain.model.PaymentProvider;

/**
 * Port IN — traitement d'un webhook provider (callback Mobile Money).
 *
 * L'implémentation DOIT vérifier la signature (A02/A08) avant toute mutation d'état,
 * et être idempotente vis-à-vis des callbacks dupliqués (A04).
 */
public interface HandlePaymentCallbackUseCase {

    /**
     * @param provider        provider source du callback (depuis le path)
     * @param rawBody         corps HTTP brut (non désérialisé)
     * @param signatureHeader header de signature à valider
     * @throws com.nexuserp.core.domain.exception.DomainException avec code SIGNATURE_INVALID si non authentique
     */
    void handleCallback(PaymentProvider provider, byte[] rawBody, String signatureHeader);
}
