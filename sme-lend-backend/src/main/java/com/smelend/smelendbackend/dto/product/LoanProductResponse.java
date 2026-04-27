package com.smelend.smelendbackend.dto.product;

import com.smelend.smelendbackend.entity.enums.StatusFlag;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class LoanProductResponse {

    private Long productId;
    private String productName;

    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    private Integer minTenorMonths;
    private Integer maxTenorMonths;

    private BigDecimal baseInterestRate;
    private StatusFlag status;

    private BigDecimal creditThreshold;
    private BigDecimal minIncomeAmount;
    private BigDecimal maxIncomeAmount;
}
