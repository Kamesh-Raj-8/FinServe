package com.smelend.smelendbackend.dto.onboarding.promoter;

import com.smelend.smelendbackend.entity.enums.KycStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PromoterResponse {
    private Long promoterId;
    private Long smeId;

    private String promoterName;
    private String mobile;
    private String email;
    private String panNumber;
    private String aadhaarNumber;
    private String din;
    private String dateOfBirth;
    private BigDecimal ownershipPct;
    private BigDecimal monthlyIncome;

    private KycStatus kycStatus;

    private Long createdByUserId;
    private String createdByEmail;
}