package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.KycRecord;
import com.smelend.smelendbackend.entity.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycRecordRepository extends JpaRepository<KycRecord, Long> {

    List<KycRecord>    findBySme_SmeId(Long smeId);
    List<KycRecord>    findByVerificationStatus(KycStatus status);
    boolean            existsBySme_SmeIdAndVerificationStatus(Long smeId, KycStatus status);
    Optional<KycRecord> findByLoanApplication_ApplicationId(Long applicationId);
}
