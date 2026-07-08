package com.m1banklab.fraud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FraudAssessmentRepository extends JpaRepository<FraudAssessment, UUID> {
    List<FraudAssessment> findTop100ByOrderByCreatedAtDesc();
}
