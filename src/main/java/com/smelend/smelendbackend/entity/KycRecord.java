package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.KycStatus;
import com.smelend.smelendbackend.entity.enums.PartyType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "kyc_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long kycId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sme_id", nullable = false)
    private Sme sme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyType partyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus verificationStatus;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "verified_by_user_id")
    private AppUser verifiedBy;

    private LocalDate verifiedDate;
}