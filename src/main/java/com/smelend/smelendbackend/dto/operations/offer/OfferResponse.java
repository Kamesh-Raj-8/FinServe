package com.smelend.smelendbackend.dto.operations.offer;

import com.smelend.smelendbackend.entity.enums.OfferStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OfferResponse {

    private Long offerId;
    private Long applicationId;

    private BigDecimal sanctionedAmount;
    private BigDecimal interestRate;
    private BigDecimal emiAmount;

    private LocalDate validUntil;
    private OfferStatus offerStatus;

    private Long createdByUserId;
    private String createdByEmail;
    private LocalDateTime createdDate;
}