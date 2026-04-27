package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.StatusFlag;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Per-product eligibility rules evaluated when a loan application is submitted.
 * Caps override the product's default min/max if stricter.
 */
@Entity
@Table(name = "eligibility_policy")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EligibilityPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct product;

    /** Human-readable description of this policy rule */
    @Column(nullable = false, length = 255)
    private String ruleName;

    /** Plain-English expression of what this rule checks */
    @Column(length = 500)
    private String ruleExpression;

    // ── Cap fields (null = use product defaults) ──────────────────
    @Column(name = "max_amount_cap", precision = 15, scale = 2)
    private BigDecimal maxAmountCap;

    @Column(name = "min_credit_score")
    private Integer minCreditScore;

    @Column(name = "min_business_vintage_months")
    private Integer minBusinessVintageMonths;

    @Column(name = "max_existing_loans")
    private Integer maxExistingLoans;

    /** Minimum DSCR (Debt Service Coverage Ratio) e.g. 1.25 */
    @Column(name = "min_dscr", precision = 5, scale = 2)
    private BigDecimal minDscr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusFlag status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = java.time.LocalDateTime.now(); }
}