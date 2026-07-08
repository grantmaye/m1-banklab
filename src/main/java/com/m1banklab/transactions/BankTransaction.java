package com.m1banklab.transactions;

import com.m1banklab.accounts.Account;
import com.m1banklab.shared.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class BankTransaction {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private Account targetAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private String description;

    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    protected BankTransaction() {
    }

    public BankTransaction(Account sourceAccount, Account targetAccount, TransactionType type, TransactionStatus status, BigDecimal amount, String description, String failureReason) {
        this.id = UUID.randomUUID();
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.type = type;
        this.status = status;
        this.amount = Money.normalize(amount);
        this.description = description;
        this.failureReason = failureReason;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceAccountId() {
        return sourceAccount == null ? null : sourceAccount.getId();
    }

    public UUID getTargetAccountId() {
        return targetAccount == null ? null : targetAccount.getId();
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
