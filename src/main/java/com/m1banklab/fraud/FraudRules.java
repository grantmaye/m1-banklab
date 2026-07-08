package com.m1banklab.fraud;

import com.m1banklab.transactions.BankTransaction;
import com.m1banklab.transactions.TransactionStatus;
import com.m1banklab.transactions.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FraudRules {
    public FraudDecision evaluate(BankTransaction transaction, long recentTransactionCount, boolean newPayee) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (transaction.getAmount().compareTo(new BigDecimal("10000.00")) > 0) {
            score += 80;
            reasons.add("amount over 10000");
        } else if (transaction.getAmount().compareTo(new BigDecimal("5000.00")) > 0) {
            score += 45;
            reasons.add("amount over 5000");
        }

        if (recentTransactionCount > 6) {
            score += 55;
            reasons.add("more than 6 transactions in 10 minutes");
        } else if (recentTransactionCount > 3) {
            score += 35;
            reasons.add("more than 3 transactions in 10 minutes");
        }

        if (transaction.getType() == TransactionType.TRANSFER && newPayee) {
            score += 35;
            reasons.add("transfer to new payee");
        }

        if (transaction.getStatus() == TransactionStatus.FAILED) {
            score += 20;
            reasons.add("failed transaction");
        }

        if (reasons.isEmpty()) {
            reasons.add("no elevated risk rules matched");
        }

        RiskLevel riskLevel = score >= 70 ? RiskLevel.HIGH : score >= 35 ? RiskLevel.MEDIUM : RiskLevel.LOW;
        return new FraudDecision(riskLevel, score, reasons);
    }
}
