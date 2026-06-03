package com.nexuserp.finance.domain.event;

import com.nexuserp.core.domain.event.DomainEvent;
import java.math.BigDecimal;

public class InvoiceValidatedEvent extends DomainEvent {
    private final String invoiceId;
    private final BigDecimal total;
    private final String currency;

    public InvoiceValidatedEvent(String tenantId, String invoiceId,
                                  BigDecimal total, String currency, String approvedBy) {
        super("nexus.finance.invoice.validated", tenantId, approvedBy, null);
        this.invoiceId = invoiceId;
        this.total = total;
        this.currency = currency;
    }

    public String getInvoiceId() { return invoiceId; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
}
