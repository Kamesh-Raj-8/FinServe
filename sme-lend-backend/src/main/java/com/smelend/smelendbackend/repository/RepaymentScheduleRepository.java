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

    @Query("SELECT s FROM RepaymentSchedule s "
         + "WHERE s.loanAccount.loanAccountId = :loanId "
         + "AND s.status <> :paidStatus "
         + "ORDER BY s.installmentNo ASC")
    List<RepaymentSchedule> findUnpaidByLoan(@Param("loanId") Long loanId,
                                              @Param("paidStatus") InstallmentStatus paidStatus);
 

    @Query("SELECT DISTINCT s.loanAccount.loanAccountId FROM RepaymentSchedule s "
         + "WHERE s.dueDate <= :today AND s.status <> :paidStatus")
    List<Long> findAllOverdueLoanIds(@Param("today") LocalDate today,
                                     @Param("paidStatus") InstallmentStatus paidStatus);

    @Query("SELECT s FROM RepaymentSchedule s JOIN FETCH s.loanAccount "
         + "WHERE s.dueDate <= :today AND s.status <> :paidStatus")
    List<RepaymentSchedule> findAllOverdue(@Param("today") LocalDate today,
                                           @Param("paidStatus") InstallmentStatus paidStatus);

    @Query("SELECT s FROM RepaymentSchedule s "
         + "WHERE s.loanAccount.loanAccountId = :loanId "
         + "AND s.dueDate <= :today AND s.status <> :paidStatus")
    List<RepaymentSchedule> findOverdueByLoan(@Param("loanId") Long loanId,
                                               @Param("today") LocalDate today,
                                               @Param("paidStatus") InstallmentStatus paidStatus);
}