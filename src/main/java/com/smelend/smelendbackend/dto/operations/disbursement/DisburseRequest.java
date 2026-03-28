package com.smelend.smelendbackend.dto.operations.disbursement;

import com.smelend.smelendbackend.entity.enums.DisbursementMode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class DisburseRequest {

    @NotNull
    private DisbursementMode mode; // NEFT/IMPS/UPI

    private String transactionRef;

    @NotNull
    private LocalDate disbursementDate;
}