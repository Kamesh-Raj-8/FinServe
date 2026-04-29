package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.ScoreBand;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Risk scorecard generated when an application is submitted.
 * Drives the auto-decision routing logic.
 */
@Entity
@Table(name = "scorecard")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Scorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scoreId;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "application_id")
    private LoanApplication application;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    /** JSON snapshot of inputs used (requestedAmount, tenor, creditScore, etc.) */
    @Column(name = "inputs_json", columnDefinition = "TEXT")
    private String inputsJson;

    /** Computed score (e.g. 300–900 for CIBIL-style) */
    @Column(name = "score_value")
    private Integer scoreValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_band", length = 20)
    private ScoreBand scoreBand;

    @Column(name = "scored_at")
    private LocalDateTime scoredAt;
}
