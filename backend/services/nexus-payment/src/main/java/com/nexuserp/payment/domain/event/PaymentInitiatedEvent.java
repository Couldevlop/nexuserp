package com.nexuserp.payment.domain.event;

import com.nexuserp.core.domain.event.DomainEvent;

import java.math.BigDecimal;

public class PaymentInitiatedEvent extends DomainEvent {

    private final String paymentId;
    private final String reference;
    private final String provider;
    private final BigDecimal amount;
    private final String currency;

    public PaymentInitiatedEvent(String tenantId, String paymentId, String reference,
                                 String provider, BigDecimal amount, String currency, String userId) {
        super("nexus.payment.initiated", tenantId, userId, null);
        this.paymentId = paymentId;
        this.reference = reference;
        this.provider = provider;
        this.amount = amount;
        this.currency = currency;
    }

    public String getPaymentId() { return paymentId; }
    public String getReference() { return reference; }
    public String getProvider() { return provider; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
}
