package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.Ptp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PtpRepository extends JpaRepository<Ptp, Long> {
    List<Ptp> findByLoanAccount_LoanAccountIdOrderByCreatedDateDesc(Long loanAccountId);
}