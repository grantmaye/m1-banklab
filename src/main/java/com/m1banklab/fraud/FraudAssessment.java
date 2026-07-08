package com.m1banklab.fraud;

import com.m1banklab.transactions.BankTransaction;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_assessments")
public class FraudAssessment {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private BankTransaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false, columnDefinition = "text")
    private String reasons;

    @Column(nullable = false)
    private Instant createdAt;

    protected FraudAssessment() {
    }

    public FraudAssessment(BankTransaction transaction, RiskLevel riskLevel, int score, String reasons) {
        this.id = UUID.randomUUID();
        this.transaction = transaction;
        this.riskLevel = riskLevel;
        this.score = score;
        this.reasons = reasons;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transaction == null ? null : transaction.getId();
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public int getScore() {
        return score;
    }

    public String getReasons() {
        return reasons;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
