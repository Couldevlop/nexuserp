package com.nexuserp.finance.domain.port.out;

import com.nexuserp.core.domain.event.DomainEvent;

/**
 * Port OUT — Publication des événements domaine vers Kafka.
 */
public interface InvoiceEventPublisher {
    void publish(DomainEvent event);
    void publishAll(java.util.List<Object> events);
}
