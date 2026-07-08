package com.m1banklab.accounts;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MoneyMovementRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @Size(max = 240) String description
) {
}
