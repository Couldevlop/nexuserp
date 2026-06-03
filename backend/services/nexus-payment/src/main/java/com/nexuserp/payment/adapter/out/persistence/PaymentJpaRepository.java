package com.nexuserp.payment.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository — requêtes paramétrées uniquement (A03 — pas de SQL concaténé).
 * Toutes les lectures authentifiées sont scoppées par tenantId (A01).
 */
public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByIdAndTenantId(UUID id, String tenantId);

    // Lecture par référence sans tenant : réservée au handler de webhook.
    Optional<PaymentJpaEntity> findByReference(String reference);

    Optional<PaymentJpaEntity> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    Page<PaymentJpaEntity> findByTenantId(String tenantId, Pageable pageable);

    Page<PaymentJpaEntity> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
}
