package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.OfferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "offer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long offerId;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private LoanApplication application;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal sanctionedAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Column(nullable = false)
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OfferStatus offerStatus;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdBy;

    @Column(nullable = false)
    private LocalDateTime createdDate;
}
