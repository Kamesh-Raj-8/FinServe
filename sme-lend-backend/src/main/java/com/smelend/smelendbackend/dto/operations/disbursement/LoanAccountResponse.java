package com.smelend.smelendbackend.dto.operations.disbursement;

import com.smelend.smelendbackend.entity.enums.LoanAccountStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class LoanAccountResponse {

    private Long loanAccountId;
    private Long applicationId;

    private String accountNumber;

    private BigDecimal principalSanctioned;
    private BigDecimal interestRate;
    private Integer tenorMonths;

    private LocalDate startDate;
    private LoanAccountStatus status;
}