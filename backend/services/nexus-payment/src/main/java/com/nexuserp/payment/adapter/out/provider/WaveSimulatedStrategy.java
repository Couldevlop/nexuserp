package com.nexuserp.payment.adapter.out.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.domain.port.out.PaymentInitiation;
import com.nexuserp.payment.domain.port.out.ProviderResponse;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stratégie simulée Wave (sandbox dev).
 * Wave fonctionne par redirection web -> renvoie un redirectUrl plutôt qu'un USSD.
 */
@Component
public class WaveSimulatedStrategy extends AbstractSimulatedProviderStrategy {

    public WaveSimulatedStrategy(ObjectMapper objectMapper, PaymentProviderProperties properties) {
        super(objectMapper, properties);
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.WAVE;
    }

    @Override
    public ProviderResponse initiateCollection(PaymentInitiation initiation) {
        String providerRef = "wave-" + UUID.randomUUID();
        String redirectUrl = "https://pay.wave.com/c/" + providerRef + "?amount=" + initiation.amount();
        log.info("[SIMULATED WAVE] initiateCollection ref={} redirectUrl issued", initiation.reference());
        return ProviderResponse.accepted(providerRef, redirectUrl, null);
    }
}
