package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.RepaymentMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repayment")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Repayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long repaymentId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_account_id")
    private LoanAccount loanAccount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepaymentMode mode;

    private String referenceNo;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false)
    private LocalDateTime createdDate;
}