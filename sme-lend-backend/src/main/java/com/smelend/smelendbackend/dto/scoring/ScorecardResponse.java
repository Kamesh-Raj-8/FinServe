package com.smelend.smelendbackend.dto.scoring;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScorecardResponse {
    private Long scoreId;
    private Long applicationId;
    private String modelVersion;
    private String inputsJson;
    private Integer scoreValue;
    private String scoreBand;
    private LocalDateTime scoredAt;
    /** Product's creditThreshold used to classify this score. */
    private Integer thresholdScore;
    /** True when scoreBand is POOR — frontend must disable the Approve button. */
    private Boolean isApproveDisabled;
}
