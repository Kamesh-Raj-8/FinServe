package com.smelend.smelendbackend.dto.operations.disbursement;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Combines Application + Offer details in one response
 * for the Operations team's "Ready to Disburse" list.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PendingDisbursementDto {

    // Application info
    private Long   applicationId;
    private String smeLegalName;
    private Long smeId;
    private String productName;
    private String applicantEmail;
    private String applicationStatus;
    private LocalDateTime submittedAt;

    // Offer info
    private Long       offerId;
    private BigDecimal sanctionedAmount;
    private BigDecimal interestRate;
    private BigDecimal emiAmount;
    private String     offerValidUntil;
    private String     offerStatus;
    private LocalDateTime offerCreatedAt;
}
