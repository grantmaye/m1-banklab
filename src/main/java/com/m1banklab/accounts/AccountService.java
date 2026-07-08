package com.m1banklab.accounts;

import com.m1banklab.audit.AuditService;
import com.m1banklab.customers.Customer;
import com.m1banklab.customers.CustomerRepository;
import com.m1banklab.shared.Money;
import com.m1banklab.shared.NotFoundException;
import com.m1banklab.transactions.TransactionResponse;
import com.m1banklab.transactions.TransactionService;
import com.m1banklab.transactions.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionService transactionService;
    private final AuditService auditService;

    public AccountService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            TransactionService transactionService,
            AuditService auditService
    ) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionService = transactionService;
        this.auditService = auditService;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + request.customerId()));

        Account account = accountRepository.save(new Account(customer, request.type(), Money.normalize(request.openingBalance())));
        auditService.record("ACCOUNT_CREATED", "ACCOUNT", account.getId(), "api", "Account opened for customer " + customer.getId());
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new NotFoundException("Account not found: " + id));
    }

    @Transactional
    public TransactionResponse deposit(UUID accountId, MoneyMovementRequest request) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        account.credit(request.amount());
        return transactionService.recordPosted(null, account, TransactionType.DEPOSIT, request.amount(), request.description());
    }

    @Transactional
    public TransactionResponse withdraw(UUID accountId, MoneyMovementRequest request) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        if (!account.canDebit(request.amount())) {
            return transactionService.recordFailedWithdrawal(account, request.amount(), "Insufficient funds");
        }
        account.debit(request.amount());
        return transactionService.recordPosted(account, null, TransactionType.WITHDRAWAL, request.amount(), request.description());
    }
}
