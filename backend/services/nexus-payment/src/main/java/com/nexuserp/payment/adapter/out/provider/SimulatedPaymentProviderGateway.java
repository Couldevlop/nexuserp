package com.nexuserp.payment.adapter.out.provider;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.domain.port.out.CallbackResult;
import com.nexuserp.payment.domain.port.out.PaymentInitiation;
import com.nexuserp.payment.domain.port.out.PaymentProviderGateway;
import com.nexuserp.payment.domain.port.out.ProviderResponse;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptateur OUT par défaut (dev/sandbox) implémentant {@link PaymentProviderGateway}.
 *
 * Dispatch vers la {@link ProviderStrategy} concrète via une strategy map keyée par
 * {@link PaymentProvider}, construite par injection de toutes les stratégies disponibles.
 *
 * EXTENSION POINT : déposer une nouvelle @Component ProviderStrategy pour un vrai
 * provider ; elle est automatiquement enregistrée ici (la dernière déclarée pour un
 * provider donné l'emporte), sans modifier ce dispatcher.
 */
@Component
public class SimulatedPaymentProviderGateway implements PaymentProviderGateway {

    private final Map<PaymentProvider, ProviderStrategy> strategies = new EnumMap<>(PaymentProvider.class);

    public SimulatedPaymentProviderGateway(List<ProviderStrategy> providerStrategies) {
        providerStrategies.forEach(s -> strategies.put(s.provider(), s));
    }

    private ProviderStrategy strategy(PaymentProvider provider) {
        ProviderStrategy s = strategies.get(provider);
        if (s == null) {
            throw DomainException.of("PROVIDER_UNSUPPORTED", "No gateway strategy for provider " + provider);
        }
        return s;
    }

    @Override
    public ProviderResponse initiateCollection(PaymentInitiation initiation) {
        return strategy(initiation.provider()).initiateCollection(initiation);
    }

    @Override
    public boolean verifyCallback(byte[] rawBody, String signatureHeader, PaymentProvider provider) {
        return strategy(provider).verifyCallback(rawBody, signatureHeader);
    }

    @Override
    public CallbackResult parseCallback(byte[] rawBody, PaymentProvider provider) {
        return strategy(provider).parseCallback(rawBody);
    }
}
