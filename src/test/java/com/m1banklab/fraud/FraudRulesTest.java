package com.m1banklab.fraud;

import com.m1banklab.transactions.BankTransaction;
import com.m1banklab.transactions.TransactionStatus;
import com.m1banklab.transactions.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FraudRulesTest {
    private final FraudRules fraudRules = new FraudRules();

    @Test
    void amountOverFiveThousandIsMediumRisk() {
        BankTransaction transaction = new BankTransaction(null, null, TransactionType.DEPOSIT, TransactionStatus.POSTED, new BigDecimal("6000.00"), null, null);

        FraudDecision decision = fraudRules.evaluate(transaction, 1, false);

        assertThat(decision.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(decision.reasons()).contains("amount over 5000");
    }

    @Test
    void amountOverTenThousandIsHighRisk() {
        BankTransaction transaction = new BankTransaction(null, null, TransactionType.WITHDRAWAL, TransactionStatus.POSTED, new BigDecimal("12000.00"), null, null);

        FraudDecision decision = fraudRules.evaluate(transaction, 1, false);

        assertThat(decision.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(decision.reasons()).contains("amount over 10000");
    }

    @Test
    void velocityAndNewPayeeLiftRisk() {
        BankTransaction transaction = new BankTransaction(null, null, TransactionType.TRANSFER, TransactionStatus.POSTED, new BigDecimal("100.00"), null, null);

        FraudDecision decision = fraudRules.evaluate(transaction, 4, true);

        assertThat(decision.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(decision.reasons()).contains("more than 3 transactions in 10 minutes", "transfer to new payee");
    }
}
