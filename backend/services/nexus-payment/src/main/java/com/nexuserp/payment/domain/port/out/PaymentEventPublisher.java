package com.nexuserp.payment.domain.port.out;

import com.nexuserp.core.domain.event.DomainEvent;

import java.util.List;

/**
 * Port OUT — publication des événements domaine paiement vers Kafka.
 */
public interface PaymentEventPublisher {
    void publish(DomainEvent event);
    void publishAll(List<Object> events);
}
