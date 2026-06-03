package com.nexuserp.core.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement domaine de base.
 * Tous les événements NexusERP héritent de cette classe.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final String eventType;
    private final String tenantId;
    private final String userId;
    private final Instant occurredAt;
    private final String traceId;

    protected DomainEvent(String eventType, String tenantId, String userId, String traceId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.tenantId = tenantId;
        this.userId = userId;
        this.occurredAt = Instant.now();
        this.traceId = traceId;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getTenantId() { return tenantId; }
    public String getUserId() { return userId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getTraceId() { return traceId; }
}
