package com.smelend.smelendbackend.util;

import com.smelend.smelendbackend.dto.fee.AppliedFeeDto;
import com.smelend.smelendbackend.entity.enums.FeeType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure-function utility for disbursement and penal fee calculations.
 * Keeps DisbursementService and PenalSchedulingService clean.
 *
 * Net disbursement = sanctionedAmount − sum(non-PENAL fees).
 * PENAL fees are NOT deducted at disbursement; they are applied on overdue.
 */
public final class DisbursementCalculator {

    private DisbursementCalculator() {}

    /**
     * Net amount actually transferred to the applicant's bank account.
     * Only PROCESSING, LEGAL, INSURANCE, TECH, OTHER are deducted upfront.
     * PENAL fees are excluded from upfront deduction.
     */
    public static BigDecimal netDisbursedAmount(BigDecimal sanctionedAmount,
                                                 List<AppliedFeeDto> appliedFees) {
        if (appliedFees == null || appliedFees.isEmpty()) return sanctionedAmount;

        BigDecimal totalUpfrontFees = appliedFees.stream()
                .filter(f -> !FeeType.PENAL.name().equals(f.getFeeType()))
                .map(AppliedFeeDto::getCalculatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal net = sanctionedAmount.subtract(totalUpfrontFees)
                         .setScale(2, RoundingMode.HALF_UP);

        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
    }

    /**
     * Returns only the PENAL fee daily rate from applied fees.
     * Used by PenalSchedulingService to compute overdue charges.
     */
    public static BigDecimal penalFeePerDay(List<AppliedFeeDto> appliedFees) {
        if (appliedFees == null) return BigDecimal.ZERO;
        return appliedFees.stream()
                .filter(f -> FeeType.PENAL.name().equals(f.getFeeType()))
                .map(AppliedFeeDto::getCalculatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Penal charge = finePerDay × DPD.
     */
    public static BigDecimal penalCharge(BigDecimal finePerDay, int dpd) {
        if (finePerDay == null || finePerDay.compareTo(BigDecimal.ZERO) <= 0 || dpd <= 0) {
            return BigDecimal.ZERO;
        }
        return finePerDay.multiply(BigDecimal.valueOf(dpd))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
