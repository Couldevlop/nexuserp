package com.nexuserp.finance.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.core.infrastructure.tenant.TenantContext;
import com.nexuserp.finance.application.command.RecordPaymentCommand;
import com.nexuserp.finance.domain.port.in.RecordPaymentUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Adapter IN — consomme nexus.payment.succeeded (publié par nexus-payment).
 * Lorsqu'un paiement Mobile Money réussit et référence une facture,
 * enregistre le règlement sur la facture correspondante.
 *
 * Idempotence : déléguée au use case (registre processed_payments) car Kafka est at-least-once.
 */
@Component
public class PaymentSucceededConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentSucceededConsumer.class);

    private final RecordPaymentUseCase recordPaymentUseCase;
    private final ObjectMapper objectMapper;

    public PaymentSucceededConsumer(RecordPaymentUseCase recordPaymentUseCase, ObjectMapper objectMapper) {
        this.recordPaymentUseCase = recordPaymentUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "nexus.payment.succeeded", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentSucceeded(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String tenantId = text(node, "tenantId");
            String invoiceId = text(node, "invoiceId");
            String paymentId = text(node, "paymentId");

            if (tenantId == null || paymentId == null) {
                log.warn("Ignoring payment.succeeded event without tenantId/paymentId");
                return;
            }
            if (invoiceId == null || invoiceId.isBlank()) {
                // Paiement non rattaché à une facture (ex. encaissement libre) — rien à faire ici.
                log.debug("payment.succeeded with no invoiceId, paymentId={} — skipped", paymentId);
                return;
            }

            JsonNode amountNode = node.get("amount");
            BigDecimal amount = amountNode != null && !amountNode.isNull() ? new BigDecimal(amountNode.asText()) : null;
            String currency = text(node, "currency");
            if (amount == null || currency == null) {
                log.warn("Ignoring payment.succeeded event with missing amount/currency, paymentId={}", paymentId);
                return;
            }

            RecordPaymentCommand cmd = new RecordPaymentCommand(
                tenantId, UUID.fromString(invoiceId), paymentId, amount, currency,
                text(node, "provider"), text(node, "externalTxId"));

            // Propage le contexte tenant pour les couches en aval (logs, audit).
            TenantContext.setTenantId(tenantId);
            try {
                recordPaymentUseCase.recordExternalPayment(cmd);
            } finally {
                TenantContext.clear();
            }
        } catch (IllegalArgumentException e) {
            // invoiceId mal formé — on log et on acquitte pour ne pas boucler indéfiniment (poison message).
            log.error("Malformed payment.succeeded payload, skipping: {}", e.getMessage());
        } catch (Exception e) {
            // Erreur transitoire (DB indispo, etc.) — on relance pour rejouer le message.
            log.error("Failed to process payment.succeeded event", e);
            throw new RuntimeException(e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }
}
