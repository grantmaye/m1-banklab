package com.m1banklab.accounts;

import com.m1banklab.customers.Customer;
import com.m1banklab.shared.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Account() {
    }

    public Account(Customer customer, AccountType type, BigDecimal openingBalance) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.customer = customer;
        this.accountNumber = generateAccountNumber();
        this.type = type;
        this.status = AccountStatus.OPEN;
        this.balance = Money.normalize(openingBalance);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customer.getId();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getType() {
        return type;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(Money.normalize(amount));
        this.updatedAt = Instant.now();
    }

    public boolean canDebit(BigDecimal amount) {
        return this.balance.compareTo(Money.normalize(amount)) >= 0;
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(Money.normalize(amount));
        this.updatedAt = Instant.now();
    }

    private static String generateAccountNumber() {
        String digits = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return String.format("%012d", Long.parseUnsignedLong(digits, 16) % 1_000_000_000_000L);
    }
}
