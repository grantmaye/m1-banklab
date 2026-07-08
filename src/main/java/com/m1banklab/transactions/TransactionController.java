package com.m1banklab.transactions;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class TransactionController {
    private final TransferService transferService;
    private final TransactionService transactionService;

    public TransactionController(TransferService transferService, TransactionService transactionService) {
        this.transferService = transferService;
        this.transactionService = transactionService;
    }

    @PostMapping("/api/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse transfer(@Valid @RequestBody TransferRequest request) {
        return transferService.transfer(request);
    }

    @GetMapping("/api/accounts/{id}/transactions")
    List<TransactionResponse> forAccount(@PathVariable UUID id) {
        return transactionService.forAccount(id);
    }
}
