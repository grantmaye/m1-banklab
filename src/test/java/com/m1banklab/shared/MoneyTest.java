package com.m1banklab.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {
    @Test
    void normalizesToTwoDecimalPlaces() {
        assertThat(Money.normalize(new BigDecimal("10.129"))).isEqualByComparingTo("10.13");
    }

    @Test
    void detectsPositiveAmounts() {
        assertThat(Money.isPositive(new BigDecimal("0.01"))).isTrue();
        assertThat(Money.isPositive(BigDecimal.ZERO)).isFalse();
    }
}
