package com.nexuserp.payment.adapter.out.provider;

import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.domain.port.out.CallbackResult;
import com.nexuserp.payment.domain.port.out.PaymentInitiation;
import com.nexuserp.payment.domain.port.out.PaymentProviderGateway;
import com.nexuserp.payment.domain.port.out.ProviderResponse;
import org.springframework.stereotype.Component;

/**
 * Adaptateur OUT implémentant {@link PaymentProviderGateway}.
 *
 * Dispatch vers la {@link ProviderStrategy} concrète résolue par le
 * {@link ProviderStrategyResolver} : RÉELLE si les identifiants du provider sont
 * configurés (variables d'environnement), SIMULÉE sinon.
 *
 * Le nom historique est conservé pour ne pas casser le câblage existant ; la sélection
 * réel/simulé est désormais déléguée au resolver (activation zéro-code).
 */
@Component
public class SimulatedPaymentProviderGateway implements PaymentProviderGateway {

    private final ProviderStrategyResolver resolver;

    public SimulatedPaymentProviderGateway(ProviderStrategyResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public ProviderResponse initiateCollection(PaymentInitiation initiation) {
        return resolver.resolve(initiation.provider()).initiateCollection(initiation);
    }

    @Override
    public boolean verifyCallback(byte[] rawBody, String signatureHeader, PaymentProvider provider) {
        return resolver.resolve(provider).verifyCallback(rawBody, signatureHeader);
    }

    @Override
    public CallbackResult parseCallback(byte[] rawBody, PaymentProvider provider) {
        return resolver.resolve(provider).parseCallback(rawBody);
    }
}
