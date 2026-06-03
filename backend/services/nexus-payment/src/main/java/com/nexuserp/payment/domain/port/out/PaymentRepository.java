package com.nexuserp.payment.domain.port.out;

import com.nexuserp.payment.domain.model.Payment;
import com.nexuserp.payment.domain.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Port OUT — persistance des paiements.
 *
 * A01 (Broken Access Control) : toutes les lectures sont scoppées par tenantId.
 */
public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id, String tenantId);

    /**
     * Recherche par référence SANS contexte tenant — utilisée par le handler de webhook,
     * où le tenant n'est pas dans le JWT mais dérivé du paiement lui-même.
     */
    Optional<Payment> findByReference(String reference);

    Optional<Payment> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    Page<Payment> findAll(String tenantId, PaymentStatus status, Pageable pageable);
}
