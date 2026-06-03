package com.nexuserp.finance.domain.port.in;

import com.nexuserp.finance.application.command.RecordPaymentCommand;

/**
 * Use Case — enregistrer un paiement encaissé hors du module finance
 * (ex. Mobile Money confirmé par nexus-payment). Doit être idempotent :
 * un même paymentId ne doit jamais être appliqué deux fois (livraison Kafka at-least-once).
 */
public interface RecordPaymentUseCase {
    void recordExternalPayment(RecordPaymentCommand command);
}
