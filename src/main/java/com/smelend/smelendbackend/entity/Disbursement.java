package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.DisbursementMode;
import com.smelend.smelendbackend.entity.enums.DisbursementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "disbursement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Disbursement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long disbursementId;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private LoanApplication application;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisbursementMode mode;

    @Column(length = 100)
    private String transactionRef;

    @Column(nullable = false)
    private LocalDate disbursementDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisbursementStatus status;
}
