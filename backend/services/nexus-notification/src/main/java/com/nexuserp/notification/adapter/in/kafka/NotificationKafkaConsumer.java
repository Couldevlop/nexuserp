package com.nexuserp.notification.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.notification.domain.model.NotificationChannel;
import com.nexuserp.notification.domain.model.NotificationMessage;
import com.nexuserp.notification.domain.service.NotificationDispatcher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    private final NotificationDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    public NotificationKafkaConsumer(NotificationDispatcher dispatcher, ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = {
            "nexus.finance.invoice.validated",
            "nexus.finance.invoice.paid",
            "nexus.finance.budget.exceeded",
            "nexus.inventory.stock.low",
            "nexus.hr.leave.approved",
            "nexus.hr.payslip.available",
            "nexus.import.complete",
            "nexus.auth.account.locked",
            "nexus.ai.anomaly.detected"
        },
        groupId = "nexus-notification",
        concurrency = "3"
    )
    public void handleNotificationEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.debug("Received event on topic={}, key={}", record.topic(), record.key());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(record.value(), Map.class);
            NotificationMessage message = buildNotificationMessage(record.topic(), payload);
            if (message != null) {
                // Le dispatcher route vers email/SMS/WhatsApp selon les canaux du message.
                // Fail-safe : il encapsule chaque canal — une erreur fournisseur ne crashe pas le consumer.
                dispatcher.dispatch(message);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing notification event topic={}: {}", record.topic(), e.getMessage(), e);
            // Ne pas acquitter — retry via Kafka retry policy
        }
    }

    @SuppressWarnings("unchecked")
    private NotificationMessage buildNotificationMessage(String topic, Map<String, Object> payload) {
        String tenantId = (String) payload.get("tenantId");
        String recipientEmail = (String) payload.getOrDefault("recipientEmail", "");
        String recipientName = (String) payload.getOrDefault("recipientName", "");
        String recipientPhone = (String) payload.getOrDefault("recipientPhone", "");
        String locale = (String) payload.getOrDefault("locale", "fr-FR");

        NotificationMessage.NotificationType type = mapTopicToType(topic);
        if (type == null) return null;

        List<NotificationChannel> channels = resolveChannels(payload, recipientEmail, recipientPhone);
        if (channels.isEmpty()) {
            log.warn("No deliverable channel (no email/phone) for topic={}", topic);
            return null;
        }

        return new NotificationMessage(
            tenantId,
            recipientEmail == null || recipientEmail.isBlank() ? null : recipientEmail,
            recipientName,
            type,
            locale,
            payload,
            recipientPhone == null || recipientPhone.isBlank() ? null : recipientPhone,
            channels
        );
    }

    /**
     * Canaux explicites via le champ "channels" du payload (ex: ["SMS","EMAIL"]),
     * sinon déduction : email présent -> EMAIL, téléphone présent -> SMS.
     */
    @SuppressWarnings("unchecked")
    private List<NotificationChannel> resolveChannels(Map<String, Object> payload,
                                                      String email, String phone) {
        List<NotificationChannel> channels = new ArrayList<>();
        Object raw = payload.get("channels");
        if (raw instanceof List<?> list && !list.isEmpty()) {
            for (Object c : list) {
                try {
                    channels.add(NotificationChannel.valueOf(String.valueOf(c).trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    log.warn("Unknown channel in payload: {}", c);
                }
            }
            return channels;
        }
        // Déduction par défaut (rétro-compatible : email seul -> EMAIL)
        if (email != null && !email.isBlank()) channels.add(NotificationChannel.EMAIL);
        if (phone != null && !phone.isBlank()) channels.add(NotificationChannel.SMS);
        return channels;
    }

    private NotificationMessage.NotificationType mapTopicToType(String topic) {
        return switch (topic) {
            case "nexus.finance.invoice.paid"       -> NotificationMessage.NotificationType.PAYMENT_RECEIVED;
            case "nexus.finance.budget.exceeded"    -> NotificationMessage.NotificationType.BUDGET_EXCEEDED;
            case "nexus.inventory.stock.low"        -> NotificationMessage.NotificationType.LOW_STOCK_ALERT;
            case "nexus.hr.leave.approved"          -> NotificationMessage.NotificationType.LEAVE_APPROVED;
            case "nexus.hr.payslip.available"       -> NotificationMessage.NotificationType.PAYSLIP_AVAILABLE;
            case "nexus.import.complete"            -> NotificationMessage.NotificationType.IMPORT_COMPLETE;
            case "nexus.auth.account.locked"        -> NotificationMessage.NotificationType.ACCOUNT_LOCKED;
            case "nexus.ai.anomaly.detected"        -> NotificationMessage.NotificationType.AI_ANOMALY_DETECTED;
            case "nexus.finance.invoice.validated"  -> NotificationMessage.NotificationType.INVOICE_DUE_REMINDER;
            default -> null;
        };
    }
}
