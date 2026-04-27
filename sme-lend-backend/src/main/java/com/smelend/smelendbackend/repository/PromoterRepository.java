package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.Promoter;
import com.smelend.smelendbackend.entity.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromoterRepository extends JpaRepository<Promoter, Long> {

    List<Promoter> findBySme_SmeId(Long smeId);

    /** All promoters of an SME whose individual kycStatus is NOT VERIFIED */
    @Query("SELECT p FROM Promoter p WHERE p.sme.smeId = :smeId AND p.kycStatus != 'VERIFIED'")
    List<Promoter> findUnverifiedBySme(@Param("smeId") Long smeId);

    /** Count of promoters of an SME with a specific kycStatus */
    long countBySme_SmeIdAndKycStatus(Long smeId, KycStatus kycStatus);

    long countBySme_SmeId(Long smeId);
}