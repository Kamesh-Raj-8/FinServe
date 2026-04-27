package com.smelend.smelendbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Join entity: KycRecord ↔ Promoter.
 *
 * Removed: promoterName snapshot column — was a duplicate of Promoter.promoterName.
 * Any code needing the promoter's name should read link.getPromoter().getPromoterName()
 * directly. This ensures a single source of truth.
 *
 * Kept: ownershipPct snapshot because ownership % can change over time and we need
 * the value that was valid when this KYC was initialized.
 */
@Entity
@Table(name = "kyc_promoter_link",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_kyc_promoter", columnNames = {"kyc_id", "promoter_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycPromoterLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "kyc_id", nullable = false)
    private KycRecord kycRecord;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "promoter_id", nullable = false)
    private Promoter promoter;

    /** true → this is the Main Promoter (mandatory docs required) */
    @Column(name = "is_main", nullable = false)
    private boolean main;

    /**
     * Snapshot of ownership % at KYC creation time.
     * Kept here because ownership % is time-sensitive financial data.
     * If null, fall back to Promoter.ownershipPct.
     */
    @Column(name = "ownership_pct", precision = 5, scale = 2)
    private BigDecimal ownershipPct;
}
