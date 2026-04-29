package com.smelend.smelendbackend.util;

import com.smelend.smelendbackend.dto.fee.AppliedFeeDto;
import com.smelend.smelendbackend.entity.enums.FeeType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class DisbursementCalculator {

    private DisbursementCalculator() {}

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

    public static BigDecimal penalFeePerDay(List<AppliedFeeDto> appliedFees) {
        if (appliedFees == null) return BigDecimal.ZERO;
        return appliedFees.stream()
                .filter(f -> FeeType.PENAL.name().equals(f.getFeeType()))
                .map(AppliedFeeDto::getCalculatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal penalCharge(BigDecimal finePerDay, int dpd) {
        if (finePerDay == null || finePerDay.compareTo(BigDecimal.ZERO) <= 0 || dpd <= 0) {
            return BigDecimal.ZERO;
        }
        return finePerDay.multiply(BigDecimal.valueOf(dpd))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
