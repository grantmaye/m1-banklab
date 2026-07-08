package com.m1banklab.accounts;

import com.m1banklab.transactions.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.create(request);
    }

    @GetMapping("/{id}")
    AccountResponse get(@PathVariable UUID id) {
        return accountService.get(id);
    }

    @PostMapping("/{id}/deposit")
    TransactionResponse deposit(@PathVariable UUID id, @Valid @RequestBody MoneyMovementRequest request) {
        return accountService.deposit(id, request);
    }

    @PostMapping("/{id}/withdraw")
    TransactionResponse withdraw(@PathVariable UUID id, @Valid @RequestBody MoneyMovementRequest request) {
        return accountService.withdraw(id, request);
    }
}
