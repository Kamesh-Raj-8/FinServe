package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.KycPromoterLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KycPromoterLinkRepository extends JpaRepository<KycPromoterLink, Long> {

    List<KycPromoterLink> findByKycRecord_KycId(Long kycId);

    /** 'main' is the field name — Lombok generates isMain() getter but JPA uses field name */
    @Query("SELECT l FROM KycPromoterLink l WHERE l.kycRecord.kycId = :kycId AND l.main = true")
    Optional<KycPromoterLink> findMainByKycId(@Param("kycId") Long kycId);

    /** Promoter links whose underlying promoter kycStatus is NOT VERIFIED */
    @Query("SELECT l FROM KycPromoterLink l "
         + "WHERE l.kycRecord.kycId = :kycId "
         + "AND l.promoter.kycStatus != 'VERIFIED'")
    List<KycPromoterLink> findUnverifiedPromotersByKyc(@Param("kycId") Long kycId);
}
