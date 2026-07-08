package com.m1banklab.fraud;

import java.util.List;

public record FraudDecision(RiskLevel riskLevel, int score, List<String> reasons) {
}
