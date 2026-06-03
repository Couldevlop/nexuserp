package com.nexuserp.finance.domain.service;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.Money;
import com.nexuserp.core.infrastructure.tenant.TenantContext;
import com.nexuserp.finance.application.command.CreateInvoiceCommand;
import com.nexuserp.finance.application.command.RecordPaymentCommand;
import com.nexuserp.finance.application.query.InvoicePageQuery;
import com.nexuserp.finance.domain.event.InvoiceCreatedEvent;
import com.nexuserp.finance.domain.model.Invoice;
import com.nexuserp.finance.domain.model.InvoiceLine;
import com.nexuserp.finance.domain.port.in.ApproveInvoiceUseCase;
import com.nexuserp.finance.domain.port.in.CreateInvoiceUseCase;
import com.nexuserp.finance.domain.port.in.GetInvoiceUseCase;
import com.nexuserp.finance.domain.port.in.RecordPaymentUseCase;
import com.nexuserp.finance.domain.port.out.InvoiceEventPublisher;
import com.nexuserp.finance.domain.port.out.InvoiceRepository;
import com.nexuserp.finance.domain.port.out.ProcessedPaymentStore;
import com.nexuserp.finance.infrastructure.compliance.ComplianceEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service domaine Finance — implémente tous les Use Cases factures.
 * La logique métier réside ici, pas dans les contrôleurs ni les entités JPA.
 */
@Service
@Transactional
public class InvoiceService implements CreateInvoiceUseCase, GetInvoiceUseCase, ApproveInvoiceUseCase, RecordPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceEventPublisher eventPublisher;
    private final ComplianceEngine complianceEngine;
    private final InvoiceNumberGenerator numberGenerator;
    private final ProcessedPaymentStore processedPaymentStore;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          InvoiceEventPublisher eventPublisher,
                          ComplianceEngine complianceEngine,
                          InvoiceNumberGenerator numberGenerator,
                          ProcessedPaymentStore processedPaymentStore) {
        this.invoiceRepository = invoiceRepository;
        this.eventPublisher = eventPublisher;
        this.complianceEngine = complianceEngine;
        this.numberGenerator = numberGenerator;
        this.processedPaymentStore = processedPaymentStore;
    }

    @Override
    public Invoice createInvoice(CreateInvoiceCommand cmd) {
        log.info("Creating invoice for tenant={}, type={}, partner={}",
            cmd.tenantId(), cmd.invoiceType(), cmd.partnerName());

        // Vérification conformité légale (PCG/SYSCOHADA selon pays tenant)
        complianceEngine.validateInvoiceCreation(cmd);

        // Génération numéro de facture unique
        String invoiceNumber = numberGenerator.generate(cmd.tenantId(), cmd.invoiceType());

        // Construction de l'agrégat via Builder
        Invoice.Builder builder = Invoice.builder()
            .tenantId(cmd.tenantId())
            .invoiceNumber(invoiceNumber)
            .type(Invoice.InvoiceType.valueOf(cmd.invoiceType()))
            .partnerId(cmd.partnerId())
            .partnerName(cmd.partnerName())
            .partnerVat(cmd.partnerVat())
            .invoiceDate(cmd.invoiceDate())
            .dueDate(cmd.dueDate())
            .currency(cmd.currency() != null ? cmd.currency() : "EUR")
            .notes(cmd.notes())
            .createdBy(cmd.createdBy());

        // Ajout des lignes
        AtomicInteger lineNum = new AtomicInteger(1);
        if (cmd.lines() != null) {
            cmd.lines().forEach(lineCmd -> builder.addLine(
                InvoiceLine.builder()
                    .lineNumber(lineNum.getAndIncrement())
                    .description(lineCmd.description())
                    .productCode(lineCmd.productCode())
                    .quantity(lineCmd.quantity())
                    .unitPrice(lineCmd.unitPrice())
                    .discountPct(lineCmd.discountPct() != null ? lineCmd.discountPct() : java.math.BigDecimal.ZERO)
                    .taxRate(lineCmd.taxRate() != null ? lineCmd.taxRate() : new java.math.BigDecimal("20.00"))
                    .accountId(lineCmd.accountId())
                    .costCenterId(lineCmd.costCenterId())
                    .build()
            ));
        }

        Invoice invoice = builder.build();

        // Transition vers SUBMITTED automatiquement si lignes présentes
        if (!invoice.getLines().isEmpty()) {
            invoice.submit();
        }

        // Persistance
        Invoice saved = invoiceRepository.save(invoice);

        // Publication événement Kafka
        eventPublisher.publish(new InvoiceCreatedEvent(
            cmd.tenantId(), saved.getId().toString(), saved.getInvoiceNumber(),
            cmd.invoiceType(), saved.getTotal().amount(), saved.getCurrency(), cmd.createdBy()
        ));

        log.info("Invoice created: id={}, number={}, total={}",
            saved.getId(), saved.getInvoiceNumber(), saved.getTotal());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getById(UUID id, String tenantId) {
        return invoiceRepository.findById(id, tenantId)
            .orElseThrow(() -> DomainException.notFound("Invoice", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Invoice> getInvoices(InvoicePageQuery query) {
        return invoiceRepository.findAll(query.tenantId(), query.status(), query.toPageable());
    }

    @Override
    public Invoice approveInvoice(UUID invoiceId, String tenantId, String approvedBy) {
        Invoice invoice = invoiceRepository.findById(invoiceId, tenantId)
            .orElseThrow(() -> DomainException.notFound("Invoice", invoiceId));

        invoice.approve(approvedBy);
        Invoice saved = invoiceRepository.save(invoice);

        // Publier tous les events domaine accumulés
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        log.info("Invoice approved: id={}, approvedBy={}", invoiceId, approvedBy);
        return saved;
    }

    @Override
    public void recordExternalPayment(RecordPaymentCommand cmd) {
        // Idempotence (Kafka at-least-once) : un paymentId n'est appliqué qu'une fois.
        if (processedPaymentStore.isProcessed(cmd.paymentId(), cmd.tenantId())) {
            log.info("Payment already applied, skipping. paymentId={}, tenant={}", cmd.paymentId(), cmd.tenantId());
            return;
        }

        Invoice invoice = invoiceRepository.findById(cmd.invoiceId(), cmd.tenantId())
            .orElseThrow(() -> DomainException.notFound("Invoice", cmd.invoiceId()));

        invoice.recordPayment(Money.of(cmd.amount(), cmd.currency()));
        Invoice saved = invoiceRepository.save(invoice);

        // Marque le paiement comme traité dans la même transaction que l'application du règlement.
        processedPaymentStore.markProcessed(cmd.paymentId(), cmd.tenantId(), cmd.invoiceId().toString());

        // Publie les events domaine accumulés (ex. InvoicePaidEvent si solde nul).
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        log.info("External payment applied: invoice={}, paymentId={}, provider={}, status={}",
            cmd.invoiceId(), cmd.paymentId(), cmd.provider(), saved.getStatus());
    }
}
