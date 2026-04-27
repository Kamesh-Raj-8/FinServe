package com.smelend.smelendbackend.dto.collections;

import com.smelend.smelendbackend.validation.DateWithinWindow;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class CreatePtpRequest {

    @NotNull
    private Long loanAccountId;

    @NotNull
    @DateWithinWindow(days = 90, futureOnly = true, message = "Promise-to-pay date must be a future date (within 90 days)")
    private LocalDate promiseDate;

    @NotNull
    @DecimalMin(value = "0.01", message = "Promised amount must be positive")
    private BigDecimal promisedAmount;

    private String notes;
}
