package com.m1banklab.audit;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String aggregateType;

    private UUID aggregateId;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false, columnDefinition = "text")
    private String details;

    @Column(nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(String eventType, String aggregateType, UUID aggregateId, String actor, String details) {
        this.id = UUID.randomUUID();
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.actor = actor;
        this.details = details;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getActor() {
        return actor;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
