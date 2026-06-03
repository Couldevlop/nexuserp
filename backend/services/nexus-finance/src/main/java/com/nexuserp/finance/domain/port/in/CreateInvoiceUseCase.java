package com.nexuserp.finance.domain.port.in;

import com.nexuserp.finance.application.command.CreateInvoiceCommand;
import com.nexuserp.finance.domain.model.Invoice;

/**
 * Port IN — Use Case de création de facture.
 */
public interface CreateInvoiceUseCase {
    Invoice createInvoice(CreateInvoiceCommand command);
}
