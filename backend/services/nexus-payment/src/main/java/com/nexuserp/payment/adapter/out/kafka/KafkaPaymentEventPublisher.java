package com.nexuserp.payment.adapter.out.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.core.domain.event.DomainEvent;
import com.nexuserp.payment.domain.port.out.PaymentEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publication Kafka des événements paiement.
 * Topics : nexus.payment.initiated / nexus.payment.succeeded / nexus.payment.failed.
 * La clé Kafka est le tenantId (partitionnement par tenant).
 */
@Component
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaPaymentEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                      ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            // eventType est déjà "nexus.payment.<x>" -> sert directement de topic.
            String topic = event.getEventType();
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.getTenantId(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event type={} to topic={}", event.getEventType(), topic, ex);
                    } else {
                        log.debug("Published event type={} to topic={}, offset={}",
                            event.getEventType(), topic, result.getRecordMetadata().offset());
                    }
                });
        } catch (Exception e) {
            log.error("Error serializing payment event type={}", event.getEventType(), e);
        }
    }

    @Override
    public void publishAll(List<Object> events) {
        events.forEach(event -> {
            if (event instanceof DomainEvent domainEvent) {
                publish(domainEvent);
            }
        });
    }
}
