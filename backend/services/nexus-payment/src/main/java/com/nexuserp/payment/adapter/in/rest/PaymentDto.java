package com.nexuserp.payment.adapter.in.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de sortie d'un paiement.
 * A09 (Logging/Exposure) : le MSISDN est exposé masqué (jamais en clair).
 */
public record PaymentDto(
    UUID id,
    String reference,
    String provider,
    String direction,
    String msisdnMasked,
    BigDecimal amount,
    String currency,
    String status,
    String externalTxId,
    UUID invoiceId,
    String failureReason,
    Instant createdAt,
    Instant updatedAt
) {}
