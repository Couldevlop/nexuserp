package com.nexuserp.payment.adapter.out.provider;

import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.domain.port.out.CallbackResult;
import com.nexuserp.payment.domain.port.out.PaymentInitiation;
import com.nexuserp.payment.domain.port.out.ProviderResponse;

/**
 * Stratégie d'intégration d'un provider Mobile Money concret.
 *
 * EXTENSION POINT : pour brancher un vrai provider (Orange Money API, Wave API, etc.),
 * implémenter cette interface dans une nouvelle classe @Component et déclarer le
 * {@link #provider()} correspondant. Le {@code SimulatedPaymentProviderGateway} construit
 * automatiquement une strategy map keyée par {@link PaymentProvider}, donc aucun autre
 * câblage n'est nécessaire — la nouvelle implémentation remplace/complète la simulation.
 */
public interface ProviderStrategy {

    PaymentProvider provider();

    ProviderResponse initiateCollection(PaymentInitiation initiation);

    /**
     * A02/A08 : doit faire une comparaison en temps constant (MessageDigest.isEqual)
     * du HMAC du corps brut contre le header de signature.
     */
    boolean verifyCallback(byte[] rawBody, String signatureHeader);

    CallbackResult parseCallback(byte[] rawBody);
}
