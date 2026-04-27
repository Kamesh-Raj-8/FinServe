package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "promoter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promoter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long promoterId;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "sme_id", nullable = false)
    private Sme sme;

    @Column(nullable = false, length = 100)
    private String promoterName;

    @Column(nullable = false, length = 15)
    private String mobile;

    @Column(length = 100)
    private String email;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "aadhaar_number", length = 12)
    private String aadhaarNumber;

    @Column(name = "din", length = 30)
    private String din;

    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal ownershipPct;

    @Column(name = "monthly_income", precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycStatus kycStatus;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdBy;
}