package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Column(nullable = false, length = 50)
    private String refType; // APPLICATION / SME / OFFER / LOAN_ACCOUNT / KYC etc.

    @Column(nullable = false)
    private Long refId;

    @ManyToOne
    @JoinColumn(name = "actor_user_id")
    private AppUser actor;

    @Column(length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdDate;
}