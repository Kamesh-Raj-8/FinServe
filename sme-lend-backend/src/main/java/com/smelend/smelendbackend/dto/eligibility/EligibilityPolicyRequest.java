package com.smelend.smelendbackend.dto.eligibility;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class EligibilityPolicyRequest {
    @NotNull private Long productId;
    @NotBlank private String ruleName;
    private String ruleExpression;
    private BigDecimal maxAmountCap;
    private Integer minCreditScore;
    private Integer minBusinessVintageMonths;
    private Integer maxExistingLoans;
    private BigDecimal minDscr;
}
