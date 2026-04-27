package com.smelend.smelendbackend.dto.compliance;

import com.smelend.smelendbackend.entity.enums.AuditAction;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLogResponse {

    private Long auditId;

    private AuditAction action;

    private String refType;
    private Long refId;

    private Long actorUserId;
    private String actorEmail;

    private String message;
    private LocalDateTime createdDate;
}