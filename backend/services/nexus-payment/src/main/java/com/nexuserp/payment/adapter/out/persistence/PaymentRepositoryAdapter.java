package com.nexuserp.payment.adapter.out.persistence;

import com.nexuserp.core.domain.value.Money;
import com.nexuserp.payment.domain.model.MobileMoneyAccount;
import com.nexuserp.payment.domain.model.Payment;
import com.nexuserp.payment.domain.model.PaymentDirection;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.domain.model.PaymentStatus;
import com.nexuserp.payment.domain.port.out.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptateur JPA pour le port OUT PaymentRepository.
 * Traduction Domain Model ↔ JPA Entity.
 */
@Repository
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpa;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = jpa.findById(payment.getId()).orElseGet(PaymentJpaEntity::new);
        toEntity(payment, entity);
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Payment> findById(UUID id, String tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByReference(String reference) {
        return jpa.findByReference(reference).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey) {
        return jpa.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).map(this::toDomain);
    }

    @Override
    public Page<Payment> findAll(String tenantId, PaymentStatus status, Pageable pageable) {
        if (status != null) {
            return jpa.findByTenantIdAndStatus(tenantId, status.name(), pageable).map(this::toDomain);
        }
        return jpa.findByTenantId(tenantId, pageable).map(this::toDomain);
    }

    // ─── Mapping ────────────────────────────────────────────────────────────────

    private void toEntity(Payment p, PaymentJpaEntity e) {
        e.setId(p.getId());
        e.setTenantId(p.getTenantId().value());
        e.setReference(p.getReference());
        e.setProvider(p.getProvider().name());
        e.setDirection(p.getDirection().name());
        e.setMsisdn(p.getAccount().value());
        e.setAmount(p.getAmount().amount());
        e.setCurrency(p.getAmount().currency());
        e.setStatus(p.getStatus().name());
        e.setExternalTxId(p.getExternalTxId());
        e.setInvoiceId(p.getInvoiceId());
        e.setIdempotencyKey(p.getIdempotencyKey());
        e.setCreatedBy(p.getCreatedBy());
        e.setFailureReason(p.getFailureReason());
        if (e.getCreatedAt() == null && p.getCreatedAt() != null) {
            e.setCreatedAt(p.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDateTime());
        }
    }

    private Payment toDomain(PaymentJpaEntity e) {
        return Payment.builder()
            .id(e.getId())
            .tenantId(e.getTenantId())
            .reference(e.getReference())
            .provider(PaymentProvider.valueOf(e.getProvider()))
            .direction(PaymentDirection.valueOf(e.getDirection()))
            .account(MobileMoneyAccount.of(e.getMsisdn()))
            .amount(Money.of(e.getAmount(), e.getCurrency()))
            .status(PaymentStatus.valueOf(e.getStatus()))
            .externalTxId(e.getExternalTxId())
            .invoiceId(e.getInvoiceId())
            .idempotencyKey(e.getIdempotencyKey())
            .createdBy(e.getCreatedBy())
            .failureReason(e.getFailureReason())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toInstant(ZoneOffset.UTC) : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant(ZoneOffset.UTC) : null)
            .build();
    }
}
