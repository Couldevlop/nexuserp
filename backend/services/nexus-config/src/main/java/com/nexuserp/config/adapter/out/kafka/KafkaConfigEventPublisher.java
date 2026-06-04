package com.nexuserp.config.adapter.out.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.config.domain.port.out.ConfigEventPublisher;
import com.nexuserp.core.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publication Kafka des événements de configuration.
 * Topic : nexus.config.changed (l'eventType sert directement de topic).
 * La clé Kafka est le tenantId (partitionnement par tenant).
 *
 * Les consommateurs s'abonnent pour invalider leur cache de config (ConfigClient).
 */
@Component
public class KafkaConfigEventPublisher implements ConfigEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfigEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaConfigEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String topic = event.getEventType(); // "nexus.config.changed"
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
            log.error("Error serializing config event type={}", event.getEventType(), e);
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
