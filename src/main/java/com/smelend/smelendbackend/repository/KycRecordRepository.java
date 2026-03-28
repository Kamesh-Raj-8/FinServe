package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.KycRecord;
import com.smelend.smelendbackend.entity.enums.KycStatus;
import com.smelend.smelendbackend.entity.enums.PartyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycRecordRepository extends JpaRepository<KycRecord, Long> {

    List<KycRecord> findBySme_SmeId(Long smeId);

    List<KycRecord> findBySme_SmeIdAndPartyType(Long smeId, PartyType partyType);

    boolean existsBySme_SmeIdAndPartyTypeAndVerificationStatus(Long smeId, PartyType partyType, KycStatus status);
}