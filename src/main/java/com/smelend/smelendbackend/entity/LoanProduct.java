package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.StatusFlag;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_product")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Column(nullable = false)
    private Integer minTenorMonths;

    @Column(nullable = false)
    private Integer maxTenorMonths;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal baseInterestRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusFlag status;
}
