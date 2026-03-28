package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.UwDecision;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "uw_review")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UwReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private LoanApplication application;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "underwriter_user_id", nullable = false)
    private AppUser underwriter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UwDecision decision;

    @Column(length = 255)
    private String summaryNote;

    @Column(nullable = false)
    private LocalDateTime createdDate;
}
