package com.smelend.smelendbackend.dto.servicing.schedule;

import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {

    private Long scheduleId;
    private Long loanAccountId;

    private Integer installmentNo;
    private LocalDate dueDate;

    private BigDecimal principalDue;
    private BigDecimal interestDue;
    private BigDecimal totalDue;

    private BigDecimal amountPaid;
    private BigDecimal balanceDue;

    private InstallmentStatus status;

    /** Cumulative penal charges added to this installment */
    private BigDecimal penalAmount;
}