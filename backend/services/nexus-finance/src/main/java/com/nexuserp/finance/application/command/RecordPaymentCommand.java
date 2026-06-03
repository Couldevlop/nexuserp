package com.nexuserp.finance.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Commande d'enregistrement d'un paiement externe (Mobile Money via nexus-payment).
 * Déclenchée par la consommation de l'événement Kafka nexus.payment.succeeded.
 */
public record RecordPaymentCommand(
    String tenantId,
    UUID invoiceId,
    String paymentId,
    BigDecimal amount,
    String currency,
    String provider,
    String externalTxId
) {}
