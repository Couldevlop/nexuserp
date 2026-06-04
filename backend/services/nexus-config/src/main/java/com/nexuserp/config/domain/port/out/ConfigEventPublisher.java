package com.nexuserp.config.domain.port.out;

import com.nexuserp.core.domain.event.DomainEvent;

import java.util.List;

/** Port OUT — publication des événements de configuration (invalidation de cache amont). */
public interface ConfigEventPublisher {

    void publish(DomainEvent event);

    void publishAll(List<Object> events);
}
