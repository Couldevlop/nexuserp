package com.nexuserp.payment.domain.port.out;

import com.nexuserp.payment.domain.model.PaymentProvider;

import java.math.BigDecimal;

/**
 * Données nécessaires au provider pour démarrer une collecte Mobile Money.
 * Record immuable — agnostique du provider concret.
 */
public record PaymentInitiation(
    String tenantId,
    String reference,
    PaymentProvider provider,
    String msisdn,
    BigDecimal amount,
    String currency,
    String description,
    String callbackUrl
) {}
