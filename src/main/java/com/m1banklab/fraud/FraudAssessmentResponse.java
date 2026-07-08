package com.m1banklab.fraud;

import java.time.Instant;
import java.util.UUID;

public record FraudAssessmentResponse(
        UUID id,
        UUID transactionId,
        RiskLevel riskLevel,
        int score,
        String reasons,
        Instant createdAt
) {
    static FraudAssessmentResponse from(FraudAssessment assessment) {
        return new FraudAssessmentResponse(
                assessment.getId(),
                assessment.getTransactionId(),
                assessment.getRiskLevel(),
                assessment.getScore(),
                assessment.getReasons(),
                assessment.getCreatedAt()
        );
    }
}
