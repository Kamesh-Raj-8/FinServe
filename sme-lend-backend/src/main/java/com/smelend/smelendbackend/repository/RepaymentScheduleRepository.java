package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.RepaymentSchedule;
import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    List<RepaymentSchedule> findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(Long loanAccountId);

    /** Unpaid installments for a specific loan, oldest first */
    @Query("SELECT s FROM RepaymentSchedule s "
         + "WHERE s.loanAccount.loanAccountId = :loanId "
         + "AND s.status <> :paidStatus "
         + "ORDER BY s.installmentNo ASC")
    List<RepaymentSchedule> findUnpaidByLoan(@Param("loanId") Long loanId,
                                              @Param("paidStatus") InstallmentStatus paidStatus);

    /**
     * Distinct loan account IDs that have at least one overdue unpaid installment.
     * Used by listAllDelinquencies() to identify which loans need a DPD refresh.
     */
    @Query("SELECT DISTINCT s.loanAccount.loanAccountId FROM RepaymentSchedule s "
         + "WHERE s.dueDate < :today AND s.status <> :paidStatus")
    List<Long> findAllOverdueLoanIds(@Param("today") LocalDate today,
                                     @Param("paidStatus") InstallmentStatus paidStatus);

    /**
     * All overdue installments across all loans, with loanAccount eagerly joined.
     * Used by PenalSchedulingService.
     */
    @Query("SELECT s FROM RepaymentSchedule s JOIN FETCH s.loanAccount "
         + "WHERE s.dueDate < :today AND s.status <> :paidStatus")
    List<RepaymentSchedule> findAllOverdue(@Param("today") LocalDate today,
                                           @Param("paidStatus") InstallmentStatus paidStatus);

    /** Overdue installments for a specific loan */
    @Query("SELECT s FROM RepaymentSchedule s "
         + "WHERE s.loanAccount.loanAccountId = :loanId "
         + "AND s.dueDate < :today AND s.status <> :paidStatus")
    List<RepaymentSchedule> findOverdueByLoan(@Param("loanId") Long loanId,
                                               @Param("today") LocalDate today,
                                               @Param("paidStatus") InstallmentStatus paidStatus);
}