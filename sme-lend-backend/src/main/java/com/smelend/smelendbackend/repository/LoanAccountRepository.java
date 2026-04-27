package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
    Optional<LoanAccount> findByApplication_ApplicationId(Long applicationId);

    java.util.List<LoanAccount> findByStatus(com.smelend.smelendbackend.entity.enums.LoanAccountStatus status);
    Optional<LoanAccount> findByAccountNumber(String accountNumber);
}