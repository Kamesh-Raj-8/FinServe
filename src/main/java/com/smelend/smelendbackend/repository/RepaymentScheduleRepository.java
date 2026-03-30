package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {
    List<RepaymentSchedule> findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(Long loanAccountId);
}