package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.ChargeType;
import com.smelend.smelendbackend.entity.enums.ChargeStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Financial charge posted to a LoanAccount.
 * Sources: processing fee at disbursal, penal charges for overdue EMIs,
 * prepayment charges, etc.
 */
@Entity
@Table(name = "charge")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chargeId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 30)
    private ChargeType chargeType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Description / reason for the charge */
    @Column(length = 255)
    private String description;

    @Column(name = "charge_date")
    private LocalDate chargeDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = java.time.LocalDateTime.now(); }
}
