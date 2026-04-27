package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.LoanApplication;
import com.smelend.smelendbackend.entity.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByCreatedBy_UserId(Long userId);

    List<LoanApplication> findByStatus(ApplicationStatus status);
}