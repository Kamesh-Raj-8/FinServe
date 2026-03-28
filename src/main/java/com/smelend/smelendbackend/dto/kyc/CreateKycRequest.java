package com.smelend.smelendbackend.dto.kyc;

import com.smelend.smelendbackend.entity.enums.PartyType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateKycRequest {

    @NotNull
    private Long smeId;

    @NotNull
    private PartyType partyType; // BUSINESS / APPLICANT / PROMOTER
}