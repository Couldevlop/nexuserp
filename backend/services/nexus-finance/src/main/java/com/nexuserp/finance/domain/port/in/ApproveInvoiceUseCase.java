package com.nexuserp.finance.domain.port.in;

import com.nexuserp.finance.domain.model.Invoice;

import java.util.UUID;

public interface ApproveInvoiceUseCase {
    Invoice approveInvoice(UUID invoiceId, String tenantId, String approvedBy);
}
