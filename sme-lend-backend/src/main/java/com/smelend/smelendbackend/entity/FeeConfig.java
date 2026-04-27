package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.FeeType;
import com.smelend.smelendbackend.entity.enums.FeeMode;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fee schedule tied to a LoanProduct.
 * Applied at disbursement (Processing/Insurance/Tech fees).
 * At disbursal: net_disbursed = sanctionedAmount - sum(applicable fees).
 */
@Entity
@Table(name = "fee_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feeId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct product;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 30)
    private FeeType feeType;

    /** FLAT = fixed INR amount; PERCENT = % of sanctionedAmount */
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_mode", nullable = false, length = 10)
    private FeeMode feeMode;

    /** The fee value — either INR flat amount or percentage (e.g. 2.5 = 2.5%) */
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal value;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusFlag status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = java.time.LocalDateTime.now(); }
}