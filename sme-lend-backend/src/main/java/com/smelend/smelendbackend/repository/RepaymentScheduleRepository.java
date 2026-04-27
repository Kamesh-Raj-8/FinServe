package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.RepaymentSchedule;
import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    List<RepaymentSchedule> findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(Long loanAccountId);

    /** First unpaid installment for a loan account */
    @Query("SELECT s FROM RepaymentSchedule s WHERE s.loanAccount.loanAccountId = :loanId "
         + "AND s.status != 'PAID' ORDER BY s.installmentNo ASC")
    List<RepaymentSchedule> findUnpaidByLoan(@Param("loanId") Long loanId);

    /** All overdue installments (dueDate < today AND not PAID) across all loans */
    @Query("SELECT s FROM RepaymentSchedule s "
         + "WHERE s.dueDate < :today AND s.status != 'PAID'")
    List<RepaymentSchedule> findAllOverdue(@Param("today") LocalDate today);

    /** Overdue installments for a specific loan */
    @Query("SELECT s FROM RepaymentSchedule s "
         + "WHERE s.loanAccount.loanAccountId = :loanId "
         + "AND s.dueDate < :today AND s.status != 'PAID'")
    List<RepaymentSchedule> findOverdueByLoan(@Param("loanId") Long loanId,
                                               @Param("today") LocalDate today);
}
