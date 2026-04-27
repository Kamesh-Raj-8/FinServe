package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.DecisionPath;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Auto-decisioning outcome recorded after scoring.
 * Path determines what happens next to the application.
 */
@Entity
@Table(name = "decision")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long decisionId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id")
    private LoanApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "path", nullable = false, length = 30)
    private DecisionPath path;

    /** Human-readable reason for the decision */
    @Column(length = 500)
    private String reason;

    /** JSON list of rule names that triggered this decision */
    @Column(name = "triggered_rules", columnDefinition = "TEXT")
    private String triggeredRules;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}
