package com.m1banklab.transactions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID sourceAccountId,
        UUID targetAccountId,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        String description,
        String failureReason,
        Instant createdAt
) {
    public static TransactionResponse from(BankTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSourceAccountId(),
                transaction.getTargetAccountId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getFailureReason(),
                transaction.getCreatedAt()
        );
    }
}
