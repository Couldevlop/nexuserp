package com.nexuserp.payment.domain.port.out;

import com.nexuserp.payment.domain.model.PaymentProvider;

/**
 * Port OUT — abstraction des fournisseurs Mobile Money (Orange Money, Wave, MTN, Moov).
 * Les implémentations concrètes vivent dans adapter/out/provider et sont sélectionnées
 * par {@link PaymentProvider} via une strategy map (extension point).
 */
public interface PaymentProviderGateway {

    /**
     * Démarre une collecte Mobile Money auprès du provider.
     */
    ProviderResponse initiateCollection(PaymentInitiation initiation);

    /**
     * Vérifie l'authenticité d'un webhook (A02/A08).
     * Implémentation : HMAC-SHA256 du corps brut comparé en temps constant
     * (MessageDigest.isEqual) au header de signature, secret par provider depuis config.
     *
     * @param rawBody         corps HTTP brut (NON désérialisé)
     * @param signatureHeader valeur du header de signature fourni par le provider
     * @param provider        provider source du callback
     * @return true si la signature est valide
     */
    boolean verifyCallback(byte[] rawBody, String signatureHeader, PaymentProvider provider);

    /**
     * Parse un webhook (déjà authentifié) en {@link CallbackResult} normalisé.
     */
    CallbackResult parseCallback(byte[] rawBody, PaymentProvider provider);
}
