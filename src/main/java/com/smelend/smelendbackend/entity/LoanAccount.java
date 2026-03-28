package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.LoanAccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_account")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loanAccountId;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private LoanApplication application;

    @Column(nullable = false, length = 30, unique = true)
    private String accountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalSanctioned;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private Integer tenorMonths;

    @Column(nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanAccountStatus status;
}
