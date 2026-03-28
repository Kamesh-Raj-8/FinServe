package com.smelend.smelendbackend.dto.collections;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class CreatePtpRequest {

    @NotNull
    private Long loanAccountId;

    @NotNull
    private LocalDate promiseDate;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal promisedAmount;

    private String notes;
}