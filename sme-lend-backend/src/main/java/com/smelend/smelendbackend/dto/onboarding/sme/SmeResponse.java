package com.smelend.smelendbackend.dto.onboarding.sme;

import com.smelend.smelendbackend.entity.enums.BusinessType;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SmeResponse {
    private Long smeId;
    private String legalName;
    private String tradeName;
    private String registrationNo;
    private BusinessType businessType;
    private String industry;
    private String address;
    private String gstNo;
    private StatusFlag status;

    private Long createdByUserId;
    private String createdByEmail;
}