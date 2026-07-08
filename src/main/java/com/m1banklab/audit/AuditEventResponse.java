package com.m1banklab.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        String actor,
        String details,
        Instant createdAt
) {
    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getActor(),
                event.getDetails(),
                event.getCreatedAt()
        );
    }
}
