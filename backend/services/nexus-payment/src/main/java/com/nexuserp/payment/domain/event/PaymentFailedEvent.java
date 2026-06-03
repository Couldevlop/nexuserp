package com.nexuserp.payment.domain.event;

import com.nexuserp.core.domain.event.DomainEvent;

import java.math.BigDecimal;

public class PaymentFailedEvent extends DomainEvent {

    private final String paymentId;
    private final String reference;
    private final String provider;
    private final BigDecimal amount;
    private final String currency;
    private final String failureReason;

    public PaymentFailedEvent(String tenantId, String paymentId, String reference,
                              String provider, BigDecimal amount, String currency, String failureReason) {
        super("nexus.payment.failed", tenantId, null, null);
        this.paymentId = paymentId;
        this.reference = reference;
        this.provider = provider;
        this.amount = amount;
        this.currency = currency;
        this.failureReason = failureReason;
    }

    public String getPaymentId() { return paymentId; }
    public String getReference() { return reference; }
    public String getProvider() { return provider; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getFailureReason() { return failureReason; }
}
