package com.nexuserp.finance.adapter.out.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.core.domain.event.DomainEvent;
import com.nexuserp.finance.domain.port.out.InvoiceEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KafkaInvoiceEventPublisher implements InvoiceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaInvoiceEventPublisher.class);
    private static final String TOPIC_PREFIX = "nexus.finance.";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaInvoiceEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                       ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String topic = TOPIC_PREFIX + event.getEventType().replace("nexus.finance.", "");
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
            log.error("Error serializing event type={}", event.getEventType(), e);
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
