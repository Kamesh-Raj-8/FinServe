package com.smelend.smelendbackend.dto.scoring;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DecisionResponse {
    private Long decisionId;
    private Long applicationId;
    private String path;
    private String reason;
    private String triggeredRules;
    private LocalDateTime decidedAt;
}
