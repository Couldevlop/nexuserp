package com.nexuserp.finance.application.query;

import java.util.UUID;

public record GetInvoiceQuery(UUID id, String tenantId) {}
