package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepaymentRepository extends JpaRepository<Repayment, Long> {
    List<Repayment> findByLoanAccount_LoanAccountIdOrderByPaymentDateDesc(Long loanAccountId);
}