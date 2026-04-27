package com.smelend.smelendbackend.dto.kyc;

import com.smelend.smelendbackend.entity.enums.KycStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class KycResponse {

    private Long      kycId;

    // Application context
    private Long      loanApplicationId;

    // SME
    private Long      smeId;
    private String    smeLegalName;

    // Primary applicant
    private Long      applicantId;
    private String    applicantEmail;
    private String    applicantFullName;

    // Promoter snapshot list (main first)
    private List<KycPromoterDto> promoters;
    /** Name of the main promoter, derived at DTO build time from promoter links */
    private String    mainPromoterName;

    // Creator (controls edit permissions)
    private Long      createdByUserId;
    private String    createdByEmail;

    // Status
    private KycStatus verificationStatus;
    private String    notes;

    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private Long          verifiedByUserId;
    private String        verifiedByEmail;

    // Convenience: whether current caller can edit (resolved in service/controller)
    private boolean   canEdit;
}
