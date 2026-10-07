package com.m1banklab.transactions;

import com.m1banklab.accounts.Account;
import com.m1banklab.accounts.AccountRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class TransferServiceTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void locksBothDirectionsInSameOrder(boolean forward) {
        var lowId = new UUID(0, 1);
        var highId = new UUID(0, 2);
        var low = mock(Account.class);
        var high = mock(Account.class);
        var repository = mock(AccountRepository.class);
        var transactions = mock(TransactionService.class);
        when(repository.findByIdForUpdate(lowId)).thenReturn(Optional.of(low));
        when(repository.findByIdForUpdate(highId)).thenReturn(Optional.of(high));
        when(low.canDebit(any())).thenReturn(true);
        when(high.canDebit(any())).thenReturn(true);
        var amount = new BigDecimal("25.00");
        new TransferService(repository, transactions).transfer(new TransferRequest(
                forward ? lowId : highId, forward ? highId : lowId, amount, "synthetic transfer"));
        var order = inOrder(repository);
        order.verify(repository).findByIdForUpdate(lowId);
        order.verify(repository).findByIdForUpdate(highId);
        verify(forward ? low : high).debit(amount);
        verify(forward ? high : low).credit(amount);
    }
}
