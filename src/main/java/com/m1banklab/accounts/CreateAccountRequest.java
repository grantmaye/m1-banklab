package com.m1banklab.accounts;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(
        @NotNull UUID customerId,
        @NotNull AccountType type,
        @NotNull @DecimalMin(value = "0.00") BigDecimal openingBalance
) {
}
