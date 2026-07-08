package com.m1banklab.fraud;

import com.m1banklab.audit.AuditService;
import com.m1banklab.notifications.NotificationService;
import com.m1banklab.transactions.BankTransaction;
import com.m1banklab.transactions.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FraudService {
    private final FraudAssessmentRepository fraudAssessmentRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final FraudRules fraudRules = new FraudRules();

    public FraudService(
            FraudAssessmentRepository fraudAssessmentRepository,
            TransactionRepository transactionRepository,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.fraudAssessmentRepository = fraudAssessmentRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public FraudAssessmentResponse assess(BankTransaction transaction) {
        long recentCount = 0;
        if (transaction.getSourceAccountId() != null) {
            recentCount = transactionRepository.countRecentForAccount(transaction.getSourceAccountId(), Instant.now().minus(10, ChronoUnit.MINUTES));
        }

        boolean newPayee = transaction.getSourceAccountId() != null
                && transaction.getTargetAccountId() != null
                && transactionRepository.countPriorTransfersToPayee(
                        transaction.getSourceAccountId(),
                        transaction.getTargetAccountId(),
                        transaction.getId()
                ) == 0;

        FraudDecision decision = fraudRules.evaluate(transaction, recentCount, newPayee);
        FraudAssessment assessment = fraudAssessmentRepository.save(new FraudAssessment(
                transaction,
                decision.riskLevel(),
                decision.score(),
                String.join("; ", decision.reasons())
        ));

        auditService.record("FRAUD_ASSESSED", "TRANSACTION", transaction.getId(), "fraud-engine", assessment.getReasons());
        if (decision.riskLevel() == RiskLevel.HIGH) {
            notificationService.recordSystemNotification("High-risk transaction detected", "Transaction " + transaction.getId() + " scored " + decision.score());
        }

        return FraudAssessmentResponse.from(assessment);
    }

    @Transactional(readOnly = true)
    public List<FraudAssessmentResponse> latest() {
        return fraudAssessmentRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(FraudAssessmentResponse::from)
                .toList();
    }
}
