package com.nexuserp.payment.adapter.out.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties;
import org.springframework.stereotype.Component;

/** Stratégie simulée MTN MoMo (sandbox dev). */
@Component
public class MtnMomoSimulatedStrategy extends AbstractSimulatedProviderStrategy {

    public MtnMomoSimulatedStrategy(ObjectMapper objectMapper, PaymentProviderProperties properties) {
        super(objectMapper, properties);
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.MTN_MOMO;
    }
}
