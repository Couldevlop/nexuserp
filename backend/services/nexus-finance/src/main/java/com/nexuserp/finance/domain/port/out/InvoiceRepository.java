package com.nexuserp.finance.domain.port.out;

import com.nexuserp.finance.domain.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port OUT — Persistance des factures.
 * L'implémentation se trouve dans adapter/out/persistence.
 */
public interface InvoiceRepository {
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(UUID id, String tenantId);
    Page<Invoice> findAll(String tenantId, Invoice.InvoiceStatus status, Pageable pageable);
    List<Invoice> findOverdueInvoices(String tenantId, LocalDate beforeDate);
    boolean existsByInvoiceNumber(String invoiceNumber, String tenantId);
    void delete(UUID id, String tenantId);
}
