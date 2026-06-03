package com.nexuserp.payment.adapter.out.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties;
import org.springframework.stereotype.Component;

/** Stratégie simulée Orange Money (sandbox dev). */
@Component
public class OrangeMoneySimulatedStrategy extends AbstractSimulatedProviderStrategy {

    public OrangeMoneySimulatedStrategy(ObjectMapper objectMapper, PaymentProviderProperties properties) {
        super(objectMapper, properties);
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.ORANGE_MONEY;
    }
}
