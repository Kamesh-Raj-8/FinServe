package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "application")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "sme_id", nullable = false)
    private Sme sme;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct product;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(nullable = false)
    private Integer tenorMonths;

    @Column(length = 255)
    private String purposeNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApplicationStatus status;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdBy;

    @Column(nullable = false)
    private LocalDateTime createdDate;
}