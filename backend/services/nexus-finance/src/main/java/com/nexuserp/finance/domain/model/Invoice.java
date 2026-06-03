package com.nexuserp.finance.domain.model;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.Money;
import com.nexuserp.core.domain.value.TenantId;
import com.nexuserp.finance.domain.event.InvoiceCreatedEvent;
import com.nexuserp.finance.domain.event.InvoiceValidatedEvent;
import com.nexuserp.finance.domain.event.InvoicePaidEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agrégat Facture — Domaine Finance.
 * Rich domain model (pas d'entité anémique).
 * Gère les règles métier : TVA, validation, paiement, états.
 */
public class Invoice {

    public enum InvoiceType { CUSTOMER, SUPPLIER, CREDIT_NOTE, DEBIT_NOTE }
    public enum InvoiceStatus { DRAFT, SUBMITTED, APPROVED, PAID, CANCELLED, OVERDUE }

    private final UUID id;
    private final TenantId tenantId;
    private final String invoiceNumber;
    private final InvoiceType type;
    private InvoiceStatus status;

    // Partenaire
    private UUID partnerId;
    private String partnerName;
    private String partnerVat;

    // Dates
    private LocalDate invoiceDate;
    private LocalDate dueDate;

    // Montants
    private final String currency;
    private Money subtotal;
    private Money taxAmount;
    private Money total;
    private Money amountPaid;

    // Lignes
    private final List<InvoiceLine> lines;

    // Journalisation
    private UUID journalEntryId;
    private String createdBy;
    private String notes;

    // Domain events
    private final List<Object> domainEvents;

    // Builder constructor — privé pour forcer l'usage du builder
    private Invoice(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.tenantId = builder.tenantId;
        this.invoiceNumber = builder.invoiceNumber;
        this.type = builder.type;
        this.status = InvoiceStatus.DRAFT;
        this.partnerId = builder.partnerId;
        this.partnerName = builder.partnerName;
        this.partnerVat = builder.partnerVat;
        this.invoiceDate = builder.invoiceDate;
        this.dueDate = builder.dueDate;
        this.currency = builder.currency;
        this.createdBy = builder.createdBy;
        this.notes = builder.notes;
        this.lines = new ArrayList<>();
        this.domainEvents = new ArrayList<>();

        // Calcul initial si lignes fournies
        builder.lines.forEach(this::addLine);
    }

    // ─── Méthodes domaine ───────────────────────────────────────────────────

    public void addLine(InvoiceLine line) {
        if (status != InvoiceStatus.DRAFT) {
            throw DomainException.invalidState("Invoice", status.name(), "DRAFT");
        }
        lines.add(line);
        recalculate();
    }

    public void removeLine(UUID lineId) {
        if (status != InvoiceStatus.DRAFT) {
            throw DomainException.invalidState("Invoice", status.name(), "DRAFT");
        }
        lines.removeIf(l -> l.getId().equals(lineId));
        recalculate();
    }

    /**
     * Soumet la facture pour validation.
     * Vérifie que les lignes ne sont pas vides et les montants cohérents.
     */
    public void submit() {
        if (status != InvoiceStatus.DRAFT) {
            throw DomainException.invalidState("Invoice", status.name(), "DRAFT");
        }
        if (lines.isEmpty()) {
            throw DomainException.of("INVOICE_NO_LINES", "Invoice must have at least one line");
        }
        if (!total.isPositive()) {
            throw DomainException.of("INVOICE_NEGATIVE_TOTAL", "Invoice total must be positive");
        }
        this.status = InvoiceStatus.SUBMITTED;
    }

    /**
     * Valide la facture (par FINANCE_MANAGER).
     */
    public void approve(String approvedBy) {
        if (status != InvoiceStatus.SUBMITTED) {
            throw DomainException.invalidState("Invoice", status.name(), "SUBMITTED");
        }
        this.status = InvoiceStatus.APPROVED;
        domainEvents.add(new InvoiceValidatedEvent(
            tenantId.value(), id.toString(), total.amount(), currency, approvedBy));
    }

    /**
     * Enregistre un paiement partiel ou total.
     */
    public void recordPayment(Money payment) {
        if (status == InvoiceStatus.CANCELLED) {
            throw DomainException.invalidState("Invoice", status.name(), "APPROVED or OVERDUE");
        }
        if (payment.isNegative() || payment.isZero()) {
            throw DomainException.of("INVALID_PAYMENT", "Payment amount must be positive");
        }
        if (!payment.currency().equals(this.currency)) {
            throw DomainException.of("CURRENCY_MISMATCH",
                "Payment currency " + payment.currency() + " does not match invoice currency " + currency);
        }

        this.amountPaid = this.amountPaid.add(payment);

        Money remaining = total.subtract(amountPaid);
        if (!remaining.isPositive()) {
            this.status = InvoiceStatus.PAID;
            domainEvents.add(new InvoicePaidEvent(tenantId.value(), id.toString(), total.amount(), currency));
        }
    }

    /**
     * Annule la facture.
     */
    public void cancel(String reason) {
        if (status == InvoiceStatus.PAID) {
            throw DomainException.of("CANNOT_CANCEL_PAID", "Cannot cancel a paid invoice. Create a credit note instead.");
        }
        this.status = InvoiceStatus.CANCELLED;
    }

    /**
     * Marque comme en retard (appelé par scheduler).
     */
    public void markOverdue() {
        if (status == InvoiceStatus.APPROVED && dueDate != null && dueDate.isBefore(LocalDate.now())) {
            this.status = InvoiceStatus.OVERDUE;
        }
    }

    /**
     * Recalcule les montants depuis les lignes.
     */
    private void recalculate() {
        BigDecimal subtotalSum = lines.stream()
            .map(l -> l.getSubtotal())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxSum = lines.stream()
            .map(l -> l.getTaxAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.subtotal = Money.of(subtotalSum, currency);
        this.taxAmount = Money.of(taxSum, currency);
        this.total = Money.of(subtotalSum.add(taxSum), currency);

        if (this.amountPaid == null) {
            this.amountPaid = Money.zero(currency);
        }
    }

    public Money getAmountDue() {
        return total.subtract(amountPaid);
    }

    public boolean isFullyPaid() {
        return !getAmountDue().isPositive();
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public InvoiceType getType() { return type; }
    public InvoiceStatus getStatus() { return status; }
    public UUID getPartnerId() { return partnerId; }
    public String getPartnerName() { return partnerName; }
    public String getPartnerVat() { return partnerVat; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public String getCurrency() { return currency; }
    public Money getSubtotal() { return subtotal; }
    public Money getTaxAmount() { return taxAmount; }
    public Money getTotal() { return total; }
    public Money getAmountPaid() { return amountPaid; }
    public List<InvoiceLine> getLines() { return Collections.unmodifiableList(lines); }
    public UUID getJournalEntryId() { return journalEntryId; }
    public String getCreatedBy() { return createdBy; }
    public String getNotes() { return notes; }

    public void setJournalEntryId(UUID journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private TenantId tenantId;
        private String invoiceNumber;
        private InvoiceType type;
        private UUID partnerId;
        private String partnerName;
        private String partnerVat;
        private LocalDate invoiceDate = LocalDate.now();
        private LocalDate dueDate;
        private String currency = "EUR";
        private String createdBy;
        private String notes;
        private final List<InvoiceLine> lines = new ArrayList<>();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(TenantId tenantId) { this.tenantId = tenantId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = TenantId.of(tenantId); return this; }
        public Builder invoiceNumber(String number) { this.invoiceNumber = number; return this; }
        public Builder type(InvoiceType type) { this.type = type; return this; }
        public Builder partnerId(UUID partnerId) { this.partnerId = partnerId; return this; }
        public Builder partnerName(String name) { this.partnerName = name; return this; }
        public Builder partnerVat(String vat) { this.partnerVat = vat; return this; }
        public Builder invoiceDate(LocalDate date) { this.invoiceDate = date; return this; }
        public Builder dueDate(LocalDate date) { this.dueDate = date; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder createdBy(String userId) { this.createdBy = userId; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder addLine(InvoiceLine line) { this.lines.add(line); return this; }

        public Invoice build() {
            validate();
            return new Invoice(this);
        }

        private void validate() {
            if (tenantId == null) throw new IllegalStateException("tenantId is required");
            if (invoiceNumber == null || invoiceNumber.isBlank()) throw new IllegalStateException("invoiceNumber is required");
            if (type == null) throw new IllegalStateException("type is required");
            if (createdBy == null || createdBy.isBlank()) throw new IllegalStateException("createdBy is required");
        }
    }
}
