package com.nexuserp.finance.adapter.out.persistence;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.Money;
import com.nexuserp.finance.domain.model.Invoice;
import com.nexuserp.finance.domain.model.InvoiceLine;
import com.nexuserp.finance.domain.port.out.InvoiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptateur JPA pour le port OUT InvoiceRepository.
 * Effectue la traduction Domain Model ↔ JPA Entity.
 */
@Repository
public class InvoiceRepositoryAdapter implements InvoiceRepository {

    private final InvoiceJpaRepository jpaRepository;

    public InvoiceRepositoryAdapter(InvoiceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceJpaEntity entity = toEntity(invoice);
        InvoiceJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Invoice> findById(UUID id, String tenantId) {
        return jpaRepository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public Page<Invoice> findAll(String tenantId, Invoice.InvoiceStatus status, Pageable pageable) {
        if (status != null) {
            return jpaRepository.findByTenantIdAndStatus(tenantId, status.name(), pageable)
                .map(this::toDomain);
        }
        return jpaRepository.findByTenantId(tenantId, pageable).map(this::toDomain);
    }

    @Override
    public List<Invoice> findOverdueInvoices(String tenantId, LocalDate beforeDate) {
        return jpaRepository.findOverdueInvoices(tenantId, beforeDate)
            .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByInvoiceNumber(String invoiceNumber, String tenantId) {
        return jpaRepository.existsByInvoiceNumberAndTenantId(invoiceNumber, tenantId);
    }

    @Override
    public void delete(UUID id, String tenantId) {
        jpaRepository.findByIdAndTenantId(id, tenantId)
            .ifPresentOrElse(
                jpaRepository::delete,
                () -> { throw DomainException.notFound("Invoice", id); }
            );
    }

    // ─── Domain ↔ Entity mapping ─────────────────────────────────────────────

    private InvoiceJpaEntity toEntity(Invoice invoice) {
        InvoiceJpaEntity entity = new InvoiceJpaEntity();
        entity.setId(invoice.getId());
        entity.setTenantId(invoice.getTenantId().value());
        entity.setInvoiceNumber(invoice.getInvoiceNumber());
        entity.setType(invoice.getType().name());
        entity.setStatus(invoice.getStatus().name());
        entity.setPartnerId(invoice.getPartnerId());
        entity.setPartnerName(invoice.getPartnerName());
        entity.setPartnerVat(invoice.getPartnerVat());
        entity.setInvoiceDate(invoice.getInvoiceDate());
        entity.setDueDate(invoice.getDueDate());
        entity.setCurrency(invoice.getCurrency());
        entity.setSubtotal(invoice.getSubtotal() != null ? invoice.getSubtotal().amount() : BigDecimal.ZERO);
        entity.setTaxAmount(invoice.getTaxAmount() != null ? invoice.getTaxAmount().amount() : BigDecimal.ZERO);
        entity.setTotal(invoice.getTotal() != null ? invoice.getTotal().amount() : BigDecimal.ZERO);
        entity.setAmountPaid(invoice.getAmountPaid() != null ? invoice.getAmountPaid().amount() : BigDecimal.ZERO);
        entity.setJournalEntryId(invoice.getJournalEntryId());
        entity.setNotes(invoice.getNotes());
        entity.setCreatedBy(invoice.getCreatedBy());

        // Lines
        entity.getLines().clear();
        invoice.getLines().forEach(line -> {
            InvoiceLineJpaEntity lineEntity = toLineEntity(line, entity);
            entity.getLines().add(lineEntity);
        });

        return entity;
    }

    private InvoiceLineJpaEntity toLineEntity(InvoiceLine line, InvoiceJpaEntity parent) {
        InvoiceLineJpaEntity entity = new InvoiceLineJpaEntity();
        entity.setId(line.getId());
        entity.setTenantId(parent.getTenantId());
        entity.setInvoice(parent);
        entity.setLineNumber(line.getLineNumber());
        entity.setDescription(line.getDescription());
        entity.setProductCode(line.getProductCode());
        entity.setQuantity(line.getQuantity());
        entity.setUnitPrice(line.getUnitPrice());
        entity.setDiscountPct(line.getDiscountPct());
        entity.setTaxRate(line.getTaxRate());
        entity.setSubtotal(line.getSubtotal());
        entity.setTaxAmount(line.getTaxAmount());
        entity.setTotal(line.getTotal());
        entity.setAccountId(line.getAccountId());
        entity.setCostCenterId(line.getCostCenterId());
        return entity;
    }

    private Invoice toDomain(InvoiceJpaEntity entity) {
        Invoice.Builder builder = Invoice.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .invoiceNumber(entity.getInvoiceNumber())
            .type(Invoice.InvoiceType.valueOf(entity.getType()))
            .partnerId(entity.getPartnerId())
            .partnerName(entity.getPartnerName())
            .partnerVat(entity.getPartnerVat())
            .invoiceDate(entity.getInvoiceDate())
            .dueDate(entity.getDueDate())
            .currency(entity.getCurrency())
            .notes(entity.getNotes())
            .createdBy(entity.getCreatedBy());

        entity.getLines().forEach(lineEntity ->
            builder.addLine(toLineDomain(lineEntity)));

        return builder.build();
    }

    private InvoiceLine toLineDomain(InvoiceLineJpaEntity entity) {
        return InvoiceLine.builder()
            .id(entity.getId())
            .lineNumber(entity.getLineNumber())
            .description(entity.getDescription())
            .productCode(entity.getProductCode())
            .quantity(entity.getQuantity())
            .unitPrice(entity.getUnitPrice())
            .discountPct(entity.getDiscountPct() != null ? entity.getDiscountPct() : BigDecimal.ZERO)
            .taxRate(entity.getTaxRate() != null ? entity.getTaxRate() : new BigDecimal("20.00"))
            .accountId(entity.getAccountId())
            .costCenterId(entity.getCostCenterId())
            .build();
    }
}
