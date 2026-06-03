package com.nexuserp.finance.domain.event;

import com.nexuserp.core.domain.event.DomainEvent;

import java.math.BigDecimal;

public class InvoiceCreatedEvent extends DomainEvent {

    private final String invoiceId;
    private final String invoiceNumber;
    private final String invoiceType;
    private final BigDecimal total;
    private final String currency;

    public InvoiceCreatedEvent(String tenantId, String invoiceId,
                                String invoiceNumber, String invoiceType,
                                BigDecimal total, String currency, String createdBy) {
        super("nexus.finance.invoice.created", tenantId, createdBy, null);
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.invoiceType = invoiceType;
        this.total = total;
        this.currency = currency;
    }

    public String getInvoiceId() { return invoiceId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getInvoiceType() { return invoiceType; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
}
