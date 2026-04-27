package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repayment_schedule", indexes = {
    @Index(name = "idx_sched_loan", columnList = "loan_account_id"),
    @Index(name = "idx_sched_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    @Column(nullable = false)
    private Integer installmentNo;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(precision = 15, scale = 2)
    private BigDecimal principalDue;

    @Column(precision = 15, scale = 2)
    private BigDecimal interestDue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceDue;

    /** Cumulative penal charges added to this installment */
    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal penalAmount = java.math.BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)

    @Column(nullable = false, length = 20)
    private InstallmentStatus status;
}