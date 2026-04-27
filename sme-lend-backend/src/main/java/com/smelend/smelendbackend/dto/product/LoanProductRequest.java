package com.smelend.smelendbackend.dto.product;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class LoanProductRequest {

    @NotBlank
    private String productName;

    @NotNull @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal minAmount;

    @NotNull @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal maxAmount;

    @NotNull @Min(1)
    private Integer minTenorMonths;

    @NotNull @Min(1)
    private Integer maxTenorMonths;

    @NotNull @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal baseInterestRate;

    /** Minimum CIBIL score required (e.g. 650) */
    @DecimalMin(value = "300") @DecimalMax(value = "900")
    private BigDecimal creditThreshold;

    /** Minimum gross annual income (INR) */
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal minIncomeAmount;

    /** Maximum gross annual income (INR) */
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal maxIncomeAmount;
}
