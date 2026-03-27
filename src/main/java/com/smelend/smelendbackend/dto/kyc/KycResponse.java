package com.smelend.smelendbackend.dto.kyc;

import com.smelend.smelendbackend.entity.enums.KycStatus;
import com.smelend.smelendbackend.entity.enums.PartyType;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class KycResponse {

    private Long kycId;
    private Long smeId;

    private PartyType partyType;
    private KycStatus verificationStatus;

    private LocalDate verifiedDate;
    private Long verifiedByUserId;
    private String verifiedByEmail;

    private String notes;
}