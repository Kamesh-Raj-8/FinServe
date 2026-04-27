package com.smelend.smelendbackend.entity;

import com.smelend.smelendbackend.entity.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * KYC snapshot tied to a LoanApplication.
 *
 * Relations:
 *  LoanApplication  ←(M:1)→  KycRecord   (one app, one KYC)
 *  KycRecord        ←(1:N)→  KycPromoterLink  (promoters linked to this KYC)
 *  KycPromoterLink  ←(M:1)→  Promoter    (live reference, no denormalization)
 *
 * Removed: mainPromoterId plain Long column (was a duplicate of what is derivable
 * via promoterLinks.stream().filter(KycPromoterLink::isMain).findFirst()).
 */
@Entity
@Table(name = "kyc_record")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long kycId;

    /** The application this KYC record belongs to */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "loan_application_id",
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private LoanApplication loanApplication;

    /** The SME this KYC covers */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sme_id", nullable = false)
    private Sme sme;

    /** Primary applicant user linked to this KYC */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "applicant_id")
    private AppUser applicant;

    /**
     * All promoters linked to this KYC.
     * Use link.isMain() to find the main promoter — no separate column needed.
     */
    @OneToMany(mappedBy = "kycRecord",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.EAGER)
    @Builder.Default
    private List<KycPromoterLink> promoterLinks = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycStatus verificationStatus;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "verified_by_user_id")
    private AppUser verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /** Helper: returns the main promoter link or null */
    public KycPromoterLink getMainPromoterLink() {
        return promoterLinks.stream()
                .filter(KycPromoterLink::isMain)
                .findFirst()
                .orElse(null);
    }
}
