package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.Disbursement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisbursementRepository extends JpaRepository<Disbursement, Long> {
    Optional<Disbursement> findByApplication_ApplicationId(Long applicationId);
}