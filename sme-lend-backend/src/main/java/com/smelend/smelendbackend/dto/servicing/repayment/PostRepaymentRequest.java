package com.smelend.smelendbackend.dto.servicing.repayment;

import com.smelend.smelendbackend.entity.enums.RepaymentMode;
import com.smelend.smelendbackend.validation.DateWithinWindow;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class PostRepaymentRequest {

    @NotNull
    private Long loanAccountId;

    /**
     * Optional: when provided, the system fetches the exact totalDue from this
     * schedule row and uses it as the payment amount — no manual entry needed.
     * If null, 'amount' must be supplied explicitly.
     */
    private Long scheduleId;

    @DecimalMin(value = "0.0", inclusive = false, message = "amount must be > 0")
    private BigDecimal amount;   // optional when scheduleId is given

    @NotNull
    private RepaymentMode mode;

    private String referenceNo;

    @NotNull
    @DateWithinWindow(days = 5, message = "Payment date must be within ±5 days of today")
    private LocalDate paymentDate;
}
