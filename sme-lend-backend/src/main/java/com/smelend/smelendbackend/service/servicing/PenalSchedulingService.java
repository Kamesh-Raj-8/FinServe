package com.smelend.smelendbackend.service.servicing;

import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.*;
import com.smelend.smelendbackend.repository.*;
import com.smelend.smelendbackend.service.charge.ChargeService;
import com.smelend.smelendbackend.service.collections.DpdService;
import com.smelend.smelendbackend.service.fee.FeeService;
import com.smelend.smelendbackend.util.DisbursementCalculator;
import com.smelend.smelendbackend.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
public class PenalSchedulingService {

    private static final Logger log = LoggerFactory.getLogger(PenalSchedulingService.class);

    private final LoanAccountRepository      loanRepo;
    private final RepaymentScheduleRepository scheduleRepo;
    private final ChargeService               chargeService;
    private final DpdService                  dpdService;
    private final NotificationService         notificationService;
    private final FeeService                  feeService;

    public PenalSchedulingService(LoanAccountRepository loanRepo,
                                   RepaymentScheduleRepository scheduleRepo,
                                   ChargeService chargeService,
                                   DpdService dpdService,
                                   NotificationService notificationService,
                                   FeeService feeService) {
        this.loanRepo            = loanRepo;
        this.scheduleRepo        = scheduleRepo;
        this.chargeService       = chargeService;
        this.dpdService          = dpdService;
        this.notificationService = notificationService;
        this.feeService          = feeService;
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processOverdueInstallments() {
        LocalDate today = LocalDate.now();
        List<LoanAccount> loans = loanRepo.findByStatusIn(
                Arrays.asList(LoanAccountStatus.ACTIVE, LoanAccountStatus.NPA));

        for (LoanAccount loan : loans) {
            try {
                processLoan(loan, today);
            } catch (Exception e) {
                log.error("[PENAL-SCHEDULER] Error processing loan #{}: {}", loan.getLoanAccountId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void processLoan(LoanAccount loan, LocalDate asOfDate) {
        List<RepaymentSchedule> overdue = scheduleRepo.findOverdueByLoan(
                loan.getLoanAccountId(), asOfDate, InstallmentStatus.PAID);

        if (overdue.isEmpty()) return;

        BigDecimal finePerDay = getFinePerDay(loan);

        int maxDpd = 0;
        BigDecimal totalNewPenalDelta = BigDecimal.ZERO;

        for (RepaymentSchedule s : overdue) {
            long daysBetween = ChronoUnit.DAYS.between(s.getDueDate(), asOfDate);
            int dpd = (daysBetween <= 0) ? 1 : (int) daysBetween;
            maxDpd = Math.max(maxDpd, dpd);

            BigDecimal calculatedTotalPenal = finePerDay.multiply(BigDecimal.valueOf(dpd)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal currentPenalInSchedule = s.getPenalAmount() != null ? s.getPenalAmount() : BigDecimal.ZERO;
            
            BigDecimal delta = calculatedTotalPenal.subtract(currentPenalInSchedule).max(BigDecimal.ZERO);

            if (delta.compareTo(BigDecimal.ZERO) > 0) {
                // UPDATE THE OVERDUE INSTALLMENT BALANCE DIRECTLY
                s.setPenalAmount(calculatedTotalPenal);
                
                // CRITICAL: Update the total and balance due of THIS installment
                s.setTotalDue(s.getTotalDue().add(delta).setScale(2, RoundingMode.HALF_UP));
                s.setBalanceDue(s.getBalanceDue().add(delta).setScale(2, RoundingMode.HALF_UP));
                
                s.setStatus(InstallmentStatus.OVERDUE);
                scheduleRepo.save(s);
                totalNewPenalDelta = totalNewPenalDelta.add(delta);
            }
        }

        if (totalNewPenalDelta.compareTo(BigDecimal.ZERO) > 0) {
            chargeService.postPenalCharge(loan, maxDpd, finePerDay);
            
            // This is kept to ensure the "Next" upcoming payment also shows the updated penalty if required
            recalculateNextEMI(loan, totalNewPenalDelta);
            
            dpdService.computeAndUpsert(loan.getLoanAccountId());
            notifyUser(loan, maxDpd, totalNewPenalDelta);
        }
    }

    private BigDecimal getFinePerDay(LoanAccount loan) {
        BigDecimal fine = BigDecimal.ZERO;
        LoanProduct product = (loan.getApplication() != null) ? loan.getApplication().getProduct() : null;
        if (product != null) {
            var fees = feeService.calculateFees(product.getProductId(), loan.getPrincipalSanctioned());
            fine = DisbursementCalculator.penalFeePerDay(fees);
        }
        // Demo Fallback
        return (fine.compareTo(BigDecimal.ZERO) <= 0) ? new BigDecimal("50.00") : fine;
    }

    @Transactional
    public void recalculateNextEMI(LoanAccount loan, BigDecimal penalToAdd) {
        List<RepaymentSchedule> schedules = scheduleRepo.findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(loan.getLoanAccountId());

        // Find the installment that is DUE (Future)
        RepaymentSchedule target = schedules.stream()
                .filter(s -> s.getStatus() == InstallmentStatus.DUE 
                          && (s.getDueDate() == null || !s.getDueDate().isBefore(LocalDate.now())))
                .findFirst()
                .orElse(null);

        // If a future installment exists, we ensure its balance reflects the penalty too 
        // (depending on how your UI sums up the totals)
        if (target != null) {
            target.setTotalDue(target.getTotalDue().add(penalToAdd).setScale(2, RoundingMode.HALF_UP));
            target.setBalanceDue(target.getBalanceDue().add(penalToAdd).setScale(2, RoundingMode.HALF_UP));
            scheduleRepo.save(target);
        }
    }

    private void notifyUser(LoanAccount loan, int dpd, BigDecimal amount) {
        if (loan.getApplication() != null && loan.getApplication().getCreatedBy() != null) {
            notificationService.notifyDelinquencyAlert(
                    loan.getApplication().getCreatedBy().getEmail(),
                    loan.getApplication().getCreatedBy().getFullName(),
                    dpd,
                    amount.toPlainString()
            );
        }
    }
}