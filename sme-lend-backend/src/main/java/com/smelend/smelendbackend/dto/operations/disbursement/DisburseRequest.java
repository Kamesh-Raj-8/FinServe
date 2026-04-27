package com.smelend.smelendbackend.dto.operations.disbursement;

import com.smelend.smelendbackend.entity.enums.DisbursementMode;
import com.smelend.smelendbackend.validation.DateWithinWindow;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Disbursement request — amount is NOT collected here.
 * Net amount is auto-calculated as:
 *   sanctionedAmount − sum(PROCESSING + LEGAL + INSURANCE + TECH fees from FeeConfig).
 */
@Getter @Setter
public class DisburseRequest {

    @NotNull(message = "mode must not be null")
    private DisbursementMode mode;

    private String transactionRef;

    @NotNull(message = "disbursementDate must not be null")
    @DateWithinWindow(days = 30, message = "Disbursement date must be within 30 days of today")
    private LocalDate disbursementDate;
}
