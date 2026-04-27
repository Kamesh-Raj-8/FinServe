package com.smelend.smelendbackend.dto.eligibility;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EligibilityPolicyResponse {
    private Long policyId;
    private Long productId;
    private String productName;
    private String ruleName;
    private String ruleExpression;
    private BigDecimal maxAmountCap;
    private Integer minCreditScore;
    private Integer minBusinessVintageMonths;
    private Integer maxExistingLoans;
    private BigDecimal minDscr;
    private String status;
    private LocalDateTime createdAt;
}
