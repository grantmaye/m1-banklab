package com.m1banklab.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private Money() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
