package com.smelend.smelendbackend.dto.operations.offer;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class CreateOfferRequest {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal sanctionedAmount;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal interestRate;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal emiAmount;

    @NotNull
    private LocalDate validUntil;
}