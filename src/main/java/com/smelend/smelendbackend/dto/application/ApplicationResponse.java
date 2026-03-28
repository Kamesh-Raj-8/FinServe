package com.smelend.smelendbackend.dto.application;

import com.smelend.smelendbackend.entity.enums.ApplicationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationResponse {

    private Long applicationId;

    private Long smeId;
    private String smeLegalName;

    private Long productId;
    private String productName;

    private BigDecimal requestedAmount;
    private Integer tenorMonths;
    private String purposeNote;

    private ApplicationStatus status;

    private Long createdByUserId;
    private String createdByEmail;

    private LocalDateTime createdDate;
}