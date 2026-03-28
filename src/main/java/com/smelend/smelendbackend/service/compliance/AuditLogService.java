package com.smelend.smelendbackend.service.compliance;

import com.smelend.smelendbackend.dto.compliance.AuditLogResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.AuditLog;
import com.smelend.smelendbackend.entity.enums.AuditAction;
import com.smelend.smelendbackend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditRepo;

    public AuditLogService(AuditLogRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    public void log(AppUser actor, AuditAction action, String refType, Long refId, String message) {
        auditRepo.save(AuditLog.builder()
                .actor(actor)
                .action(action)
                .refType(refType)
                .refId(refId)
                .message(message)
                .createdDate(LocalDateTime.now())
                .build());
    }

    public List<AuditLogResponse> listAll() {
        return auditRepo.findAll().stream().map(this::toDto).toList();
    }

    public List<AuditLogResponse> listByRef(String refType, Long refId) {
        return auditRepo.findByRefTypeAndRefIdOrderByCreatedDateDesc(refType, refId)
                .stream().map(this::toDto).toList();
    }

    public List<AuditLogResponse> listByActor(Long actorUserId) {
        return auditRepo.findByActor_UserIdOrderByCreatedDateDesc(actorUserId)
                .stream().map(this::toDto).toList();
    }

    private AuditLogResponse toDto(AuditLog a) {
        return AuditLogResponse.builder()
                .auditId(a.getAuditId())
                .action(a.getAction())
                .refType(a.getRefType())
                .refId(a.getRefId())
                .actorUserId(a.getActor() != null ? a.getActor().getUserId() : null)
                .actorEmail(a.getActor() != null ? a.getActor().getEmail() : null)
                .message(a.getMessage())
                .createdDate(a.getCreatedDate())
                .build();
    }
}