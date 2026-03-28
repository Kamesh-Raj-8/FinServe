package com.smelend.smelendbackend.dto.operations.disbursement;

import com.smelend.smelendbackend.entity.enums.DisbursementMode;
import com.smelend.smelendbackend.entity.enums.DisbursementStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DisbursementResponse {

    private Long disbursementId;
    private Long applicationId;

    private BigDecimal amount;

    private DisbursementMode mode;
    private String transactionRef;

    private LocalDate disbursementDate;
    private DisbursementStatus status;

    private LoanAccountResponse loanAccount;
}