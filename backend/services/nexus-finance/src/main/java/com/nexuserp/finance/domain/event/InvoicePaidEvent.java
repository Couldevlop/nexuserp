package com.nexuserp.finance.domain.event;

import com.nexuserp.core.domain.event.DomainEvent;
import java.math.BigDecimal;

public class InvoicePaidEvent extends DomainEvent {
    private final String invoiceId;
    private final BigDecimal total;
    private final String currency;

    public InvoicePaidEvent(String tenantId, String invoiceId, BigDecimal total, String currency) {
        super("nexus.finance.invoice.paid", tenantId, "system", null);
        this.invoiceId = invoiceId;
        this.total = total;
        this.currency = currency;
    }

    public String getInvoiceId() { return invoiceId; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
}
