package com.nexuserp.finance.domain.port.out;

/**
 * Port OUT — registre d'idempotence des paiements externes déjà appliqués.
 * Garantit l'effet exactly-once côté finance malgré la livraison at-least-once de Kafka.
 */
public interface ProcessedPaymentStore {
    boolean isProcessed(String paymentId, String tenantId);
    void markProcessed(String paymentId, String tenantId, String invoiceId);
}
