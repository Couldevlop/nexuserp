package com.nexuserp.payment.domain.model;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.Money;
import com.nexuserp.core.domain.value.TenantId;
import com.nexuserp.payment.domain.event.PaymentFailedEvent;
import com.nexuserp.payment.domain.event.PaymentInitiatedEvent;
import com.nexuserp.payment.domain.event.PaymentSucceededEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agrégat Payment — Mobile Money collection / disbursement.
 *
 * Rich domain model : la machine à états vit ici, jamais dans l'entité JPA (anémique interdit).
 *
 * OWASP :
 *  - A04 (Insecure Design) : transitions d'état explicites + idempotencyKey anti double-débit.
 *  - A09 (Logging) : msisdn exposé uniquement via MobileMoneyAccount.masked().
 */
public class Payment {

    private final UUID id;
    private final TenantId tenantId;
    private final String reference;
    private final PaymentProvider provider;
    private final PaymentDirection direction;
    private final MobileMoneyAccount account;
    private final Money amount;
    private PaymentStatus status;

    private String externalTxId;
    private final UUID invoiceId;          // lien optionnel vers nexus-finance
    private final String idempotencyKey;
    private final String createdBy;
    private String failureReason;

    private final Instant createdAt;
    private Instant updatedAt;

    private final List<Object> domainEvents;

    private Payment(Builder b) {
        this.id = b.id != null ? b.id : UUID.randomUUID();
        this.tenantId = b.tenantId;
        this.reference = b.reference;
        this.provider = b.provider;
        this.direction = b.direction != null ? b.direction : PaymentDirection.COLLECTION;
        this.account = b.account;
        this.amount = b.amount;
        this.status = b.status != null ? b.status : PaymentStatus.PENDING;
        this.externalTxId = b.externalTxId;
        this.invoiceId = b.invoiceId;
        this.idempotencyKey = b.idempotencyKey;
        this.createdBy = b.createdBy;
        this.failureReason = b.failureReason;
        this.createdAt = b.createdAt != null ? b.createdAt : Instant.now();
        this.updatedAt = b.updatedAt != null ? b.updatedAt : this.createdAt;
        this.domainEvents = new ArrayList<>();
    }

    // ─── Machine à états ──────────────────────────────────────────────────────

    /**
     * PENDING -> INITIATED. Appelé après la prise en charge par le provider.
     * @param providerRef référence de transaction renvoyée par le provider (peut servir d'externalTxId provisoire).
     */
    public void initiate(String providerRef) {
        requireStatus(PaymentStatus.PENDING, "INITIATED");
        this.status = PaymentStatus.INITIATED;
        if (providerRef != null && this.externalTxId == null) {
            this.externalTxId = providerRef;
        }
        touch();
        domainEvents.add(new PaymentInitiatedEvent(
            tenantId.value(), id.toString(), reference, provider.name(),
            amount.amount(), amount.currency(), createdBy));
    }

    /**
     * (PENDING|INITIATED) -> SUCCEEDED. Idempotent : un second appel alors que
     * le paiement est déjà SUCCEEDED est un no-op (callbacks dupliqués).
     */
    public void markSucceeded(String extTxId) {
        if (this.status == PaymentStatus.SUCCEEDED) {
            return; // A04 : idempotence sur callbacks dupliqués — aucun ré-événement.
        }
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.INITIATED) {
            throw DomainException.invalidState("Payment", status.name(), "PENDING or INITIATED");
        }
        this.status = PaymentStatus.SUCCEEDED;
        if (extTxId != null) {
            this.externalTxId = extTxId;
        }
        touch();
        domainEvents.add(new PaymentSucceededEvent(
            tenantId.value(), id.toString(), reference, provider.name(),
            externalTxId, amount.amount(), amount.currency(),
            invoiceId != null ? invoiceId.toString() : null));
    }

    /**
     * (PENDING|INITIATED) -> FAILED. Idempotent si déjà FAILED.
     */
    public void markFailed(String reason) {
        if (this.status == PaymentStatus.FAILED) {
            return; // idempotence
        }
        if (this.status == PaymentStatus.SUCCEEDED || this.status == PaymentStatus.REFUNDED) {
            throw DomainException.invalidState("Payment", status.name(), "PENDING or INITIATED");
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        touch();
        domainEvents.add(new PaymentFailedEvent(
            tenantId.value(), id.toString(), reference, provider.name(),
            amount.amount(), amount.currency(), reason));
    }

    /**
     * (PENDING|INITIATED) -> CANCELLED.
     */
    public void cancel(String reason) {
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.INITIATED) {
            throw DomainException.invalidState("Payment", status.name(), "PENDING or INITIATED");
        }
        this.status = PaymentStatus.CANCELLED;
        this.failureReason = reason;
        touch();
    }

    /**
     * SUCCEEDED -> REFUNDED.
     */
    public void markRefunded() {
        requireStatus(PaymentStatus.SUCCEEDED, "REFUNDED");
        this.status = PaymentStatus.REFUNDED;
        touch();
    }

    private void requireStatus(PaymentStatus required, String target) {
        if (this.status != required) {
            throw DomainException.invalidState("Payment", status.name(), required.name() + " (to reach " + target + ")");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public String getReference() { return reference; }
    public PaymentProvider getProvider() { return provider; }
    public PaymentDirection getDirection() { return direction; }
    public MobileMoneyAccount getAccount() { return account; }
    public Money getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getExternalTxId() { return externalTxId; }
    public UUID getInvoiceId() { return invoiceId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getCreatedBy() { return createdBy; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ─── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private TenantId tenantId;
        private String reference;
        private PaymentProvider provider;
        private PaymentDirection direction;
        private MobileMoneyAccount account;
        private Money amount;
        private PaymentStatus status;
        private String externalTxId;
        private UUID invoiceId;
        private String idempotencyKey;
        private String createdBy;
        private String failureReason;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(TenantId tenantId) { this.tenantId = tenantId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = TenantId.of(tenantId); return this; }
        public Builder reference(String reference) { this.reference = reference; return this; }
        public Builder provider(PaymentProvider provider) { this.provider = provider; return this; }
        public Builder direction(PaymentDirection direction) { this.direction = direction; return this; }
        public Builder account(MobileMoneyAccount account) { this.account = account; return this; }
        public Builder amount(Money amount) { this.amount = amount; return this; }
        public Builder status(PaymentStatus status) { this.status = status; return this; }
        public Builder externalTxId(String externalTxId) { this.externalTxId = externalTxId; return this; }
        public Builder invoiceId(UUID invoiceId) { this.invoiceId = invoiceId; return this; }
        public Builder idempotencyKey(String key) { this.idempotencyKey = key; return this; }
        public Builder createdBy(String userId) { this.createdBy = userId; return this; }
        public Builder failureReason(String reason) { this.failureReason = reason; return this; }
        public Builder createdAt(Instant ts) { this.createdAt = ts; return this; }
        public Builder updatedAt(Instant ts) { this.updatedAt = ts; return this; }

        public Payment build() {
            validate();
            return new Payment(this);
        }

        private void validate() {
            if (tenantId == null) throw DomainException.of("PAYMENT_INVALID", "tenantId is required");
            if (reference == null || reference.isBlank()) throw DomainException.of("PAYMENT_INVALID", "reference is required");
            if (provider == null) throw DomainException.of("PAYMENT_INVALID", "provider is required");
            if (account == null) throw DomainException.of("PAYMENT_INVALID", "mobile money account is required");
            if (amount == null) throw DomainException.of("PAYMENT_INVALID", "amount is required");
            if (!amount.isPositive()) throw DomainException.of("PAYMENT_INVALID", "amount must be positive");
            if (idempotencyKey == null || idempotencyKey.isBlank()) throw DomainException.of("PAYMENT_INVALID", "idempotencyKey is required");
            if (createdBy == null || createdBy.isBlank()) throw DomainException.of("PAYMENT_INVALID", "createdBy is required");
        }
    }
}
