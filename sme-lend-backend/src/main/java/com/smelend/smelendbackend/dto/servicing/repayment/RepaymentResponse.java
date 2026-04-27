package com.smelend.smelendbackend.dto.servicing.repayment;

import com.smelend.smelendbackend.entity.enums.RepaymentMode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RepaymentResponse {

    private Long repaymentId;
    private Long loanAccountId;

    private BigDecimal amount;
    private RepaymentMode mode;
    private String referenceNo;

    private LocalDate paymentDate;
    private LocalDateTime createdDate;
}