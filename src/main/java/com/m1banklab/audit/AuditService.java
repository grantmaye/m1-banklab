package com.m1banklab.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String eventType, String aggregateType, UUID aggregateId, String actor, String details) {
        auditEventRepository.save(new AuditEvent(eventType, aggregateType, aggregateId, actor, details));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> latest() {
        return auditEventRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(AuditEventResponse::from)
                .toList();
    }
}
