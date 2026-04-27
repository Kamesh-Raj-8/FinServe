package com.smelend.smelendbackend.dto.kyc;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for POST /kyc/initialize — triggers automatic KYC
 * snapshot creation from a LoanApplication's participants.
 */
@Getter @Setter
public class InitKycRequest {

    @NotNull(message = "loanApplicationId is required")
    private Long loanApplicationId;

    /** Optional notes to attach to the KYC record */
    private String notes;
}
