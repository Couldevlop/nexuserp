package com.nexuserp.payment.domain.event;

import com.nexuserp.core.domain.event.DomainEvent;

import java.math.BigDecimal;

public class PaymentSucceededEvent extends DomainEvent {

    private final String paymentId;
    private final String reference;
    private final String provider;
    private final String externalTxId;
    private final BigDecimal amount;
    private final String currency;
    private final String invoiceId;

    public PaymentSucceededEvent(String tenantId, String paymentId, String reference,
                                 String provider, String externalTxId, BigDecimal amount,
                                 String currency, String invoiceId) {
        super("nexus.payment.succeeded", tenantId, null, null);
        this.paymentId = paymentId;
        this.reference = reference;
        this.provider = provider;
        this.externalTxId = externalTxId;
        this.amount = amount;
        this.currency = currency;
        this.invoiceId = invoiceId;
    }

    public String getPaymentId() { return paymentId; }
    public String getReference() { return reference; }
    public String getProvider() { return provider; }
    public String getExternalTxId() { return externalTxId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getInvoiceId() { return invoiceId; }
}
