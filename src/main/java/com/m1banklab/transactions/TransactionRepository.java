package com.m1banklab.transactions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<BankTransaction, UUID> {
    @Query("""
            select t from BankTransaction t
            where t.sourceAccount.id = :accountId or t.targetAccount.id = :accountId
            order by t.createdAt desc
            """)
    List<BankTransaction> findForAccount(UUID accountId);

    @Query("""
            select count(t) from BankTransaction t
            where (t.sourceAccount.id = :accountId or t.targetAccount.id = :accountId)
            and t.createdAt >= :since
            """)
    long countRecentForAccount(UUID accountId, Instant since);

    @Query("""
            select count(t) from BankTransaction t
            where t.sourceAccount.id = :sourceAccountId
            and t.targetAccount.id = :targetAccountId
            and t.id <> :currentTransactionId
            """)
    long countPriorTransfersToPayee(UUID sourceAccountId, UUID targetAccountId, UUID currentTransactionId);
}
