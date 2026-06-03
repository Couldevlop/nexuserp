package com.nexuserp.finance.domain.port.in;

import com.nexuserp.finance.application.query.GetInvoiceQuery;
import com.nexuserp.finance.application.query.InvoicePageQuery;
import com.nexuserp.finance.domain.model.Invoice;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Port IN — Queries sur les factures.
 */
public interface GetInvoiceUseCase {
    Invoice getById(UUID id, String tenantId);
    Page<Invoice> getInvoices(InvoicePageQuery query);
}
