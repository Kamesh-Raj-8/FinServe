package com.smelend.smelendbackend.dto.operations.offer;

import com.smelend.smelendbackend.validation.DateWithinWindow;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class CreateOfferRequest {

    @NotNull
    @DecimalMin(value = "1", message = "Sanctioned amount must be positive")
    private BigDecimal sanctionedAmount;

    @NotNull
    @DecimalMin(value = "0.01", message = "Interest rate must be positive")
    private BigDecimal interestRate;

    @NotNull
    @DecimalMin(value = "1", message = "EMI amount must be positive")
    private BigDecimal emiAmount;

    @NotNull
    @DateWithinWindow(days = 30, futureOnly = true, message = "Offer valid-until must be a future date (today+1 to today+30)")
    private LocalDate validUntil;
}
