package com.m1banklab.notifications;

import com.m1banklab.customers.Customer;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(Customer customer, NotificationChannel channel, String subject, String body) {
        this.id = UUID.randomUUID();
        this.customer = customer;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.status = NotificationStatus.QUEUED;
        this.createdAt = Instant.now();
    }
}
