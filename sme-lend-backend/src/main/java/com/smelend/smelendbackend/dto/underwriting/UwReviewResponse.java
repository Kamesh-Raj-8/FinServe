package com.smelend.smelendbackend.dto.underwriting;

import com.smelend.smelendbackend.entity.enums.ApplicationStatus;
import com.smelend.smelendbackend.entity.enums.UwDecision;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UwReviewResponse {

    private Long reviewId;
    private Long applicationId;

    private UwDecision decision;
    private String summaryNote;

    private Long underwriterUserId;
    private String underwriterEmail;

    private LocalDateTime createdDate;

    private ApplicationStatus newApplicationStatus;
}