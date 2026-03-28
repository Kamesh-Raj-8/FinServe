package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.Delinquency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DelinquencyRepository extends JpaRepository<Delinquency, Long> {
    Optional<Delinquency> findByLoanAccount_LoanAccountId(Long loanAccountId);
}