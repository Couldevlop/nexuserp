package com.nexuserp.payment.application.command;

import com.nexuserp.payment.domain.model.PaymentDirection;
import com.nexuserp.payment.domain.model.PaymentProvider;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Commande d'initiation d'un paiement Mobile Money.
 * Record immuable.
 *
 * @param idempotencyKey clé fournie par le client (header Idempotency-Key) — anti double-débit (A04).
 *                       Si null, le service en génère une.
 */
public record InitiatePaymentCommand(
    String tenantId,
    PaymentProvider provider,
    PaymentDirection direction,
    String msisdn,
    BigDecimal amount,
    String currency,
    UUID invoiceId,
    String description,
    String idempotencyKey,
    String createdBy
) {}
