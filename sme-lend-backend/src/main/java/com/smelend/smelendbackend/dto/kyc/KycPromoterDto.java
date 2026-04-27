package com.smelend.smelendbackend.dto.kyc;

import lombok.*;

import java.math.BigDecimal;

/** Snapshot of one promoter within a KYC response. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class KycPromoterDto {
    private Long       promoterId;
    private String     promoterName;
    private BigDecimal ownershipPct;
    private boolean    main;           // true → mandatory doc subject
    private String     kycStatus;      // promoter-level KYC status
    private String     mobile;
}
