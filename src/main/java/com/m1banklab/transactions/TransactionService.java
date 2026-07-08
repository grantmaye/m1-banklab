package com.m1banklab.transactions;

import com.m1banklab.accounts.Account;
import com.m1banklab.audit.AuditService;
import com.m1banklab.fraud.FraudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;
    private final FraudService fraudService;

    public TransactionService(TransactionRepository transactionRepository, AuditService auditService, FraudService fraudService) {
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
        this.fraudService = fraudService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public TransactionResponse recordPosted(Account source, Account target, TransactionType type, BigDecimal amount, String description) {
        BankTransaction transaction = transactionRepository.save(new BankTransaction(source, target, type, TransactionStatus.POSTED, amount, description, null));
        fraudService.assess(transaction);
        auditService.record(type + "_POSTED", "TRANSACTION", transaction.getId(), "api", "Posted " + type + " for " + amount);
        return TransactionResponse.from(transaction);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public TransactionResponse recordFailedWithdrawal(Account source, BigDecimal amount, String reason) {
        BankTransaction transaction = transactionRepository.save(new BankTransaction(source, null, TransactionType.WITHDRAWAL, TransactionStatus.FAILED, amount, null, reason));
        fraudService.assess(transaction);
        auditService.record("WITHDRAWAL_FAILED", "ACCOUNT", source.getId(), "api", reason + " for attempted amount " + amount);
        return TransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> forAccount(UUID accountId) {
        return transactionRepository.findForAccount(accountId).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
