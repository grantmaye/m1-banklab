package com.m1banklab.transactions;

import com.m1banklab.accounts.Account;
import com.m1banklab.accounts.AccountRepository;
import com.m1banklab.shared.BusinessRuleException;
import com.m1banklab.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    public TransferService(AccountRepository accountRepository, TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        if (request.sourceAccountId().equals(request.targetAccountId())) {
            throw new BusinessRuleException("Source and target accounts must be different.");
        }

        Account source = accountRepository.findByIdForUpdate(request.sourceAccountId())
                .orElseThrow(() -> new NotFoundException("Source account not found: " + request.sourceAccountId()));
        Account target = accountRepository.findByIdForUpdate(request.targetAccountId())
                .orElseThrow(() -> new NotFoundException("Target account not found: " + request.targetAccountId()));

        if (!source.canDebit(request.amount())) {
            throw new BusinessRuleException("Insufficient funds for transfer.");
        }

        source.debit(request.amount());
        target.credit(request.amount());
        return transactionService.recordPosted(source, target, TransactionType.TRANSFER, request.amount(), request.description());
    }
}
