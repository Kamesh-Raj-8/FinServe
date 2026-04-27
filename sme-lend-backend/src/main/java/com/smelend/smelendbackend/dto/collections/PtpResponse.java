package com.smelend.smelendbackend.dto.collections;

import com.smelend.smelendbackend.entity.enums.PtpStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PtpResponse {

    private Long ptpId;
    private Long loanAccountId;

    private LocalDate promiseDate;
    private BigDecimal promisedAmount;

    private PtpStatus status;
    private String notes;

    private Long createdByUserId;
    private String createdByEmail;
    private LocalDateTime createdDate;
}