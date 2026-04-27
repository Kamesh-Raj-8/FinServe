package com.smelend.smelendbackend.service.servicing;

import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.entity.LoanProduct;
import com.smelend.smelendbackend.entity.RepaymentSchedule;
import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import com.smelend.smelendbackend.entity.enums.LoanAccountStatus;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import com.smelend.smelendbackend.repository.RepaymentScheduleRepository;
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
import java.util.List;

/**
 * Nightly scheduled service that:
 * 1. Detects overdue EMI installments across all ACTIVE loan accounts.
 * 2. Auto-calculates and posts penal charges (delinquencyFinePerDay × DPD).
 * 3. Adds penalty to next outstanding EMI row (recalculateNextEMI).
 * 4. Notifies the applicant via in-app + WebSocket push.
 *
 * Cron: runs at 01:00 every night.
 */
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

    /**
     * Runs at midnight every night.
     * Processes all ACTIVE loans that have overdue installments.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processOverdueInstallments() {
        LocalDate today = LocalDate.now();
        List<LoanAccount> activeLoans = loanRepo.findByStatus(LoanAccountStatus.ACTIVE);

        log.info("[PENAL-SCHEDULER] Running for {} active loans on {}", activeLoans.size(), today);

        for (LoanAccount loan : activeLoans) {
            try {
                processLoan(loan, today);
            } catch (Exception e) {
                log.error("[PENAL-SCHEDULER] Error processing loan #{}: {}", loan.getLoanAccountId(), e.getMessage());
            }
        }
    }

    /**
     * Also callable on-demand (e.g., after a repayment is posted to re-evaluate penalties).
     */
    @Transactional
    public void processLoan(LoanAccount loan, LocalDate asOfDate) {
        List<RepaymentSchedule> overdue = scheduleRepo.findOverdueByLoan(
                loan.getLoanAccountId(), asOfDate);

        if (overdue.isEmpty()) return;

        LoanProduct product = loan.getApplication() != null
                ? loan.getApplication().getProduct() : null;

        // Look up PENAL fee rate from FeeConfig (product fee table, not entity field)
        BigDecimal finePerDay = BigDecimal.ZERO;
        if (product != null) {
            var fees = feeService.calculateFees(product.getProductId(),
                    java.math.BigDecimal.valueOf(1000)); // normalised base for PENAL rate
            finePerDay = DisbursementCalculator.penalFeePerDay(fees);
        }

        int maxDpd = 0;
        BigDecimal totalNewPenal = BigDecimal.ZERO;

        for (RepaymentSchedule s : overdue) {
            int dpd = (int) ChronoUnit.DAYS.between(s.getDueDate(), asOfDate);
            maxDpd = Math.max(maxDpd, dpd);

            if (finePerDay.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dailyPenal = finePerDay.multiply(BigDecimal.valueOf(dpd))
                        .setScale(2, RoundingMode.HALF_UP);

                // Only apply incremental penal (avoid double-charging)
                BigDecimal alreadyCharged = s.getPenalAmount() != null
                        ? s.getPenalAmount() : BigDecimal.ZERO;

                BigDecimal newPenal = dailyPenal.subtract(alreadyCharged).max(BigDecimal.ZERO);

                if (newPenal.compareTo(BigDecimal.ZERO) > 0) {
                    s.setPenalAmount(dailyPenal); // update to current total
                    s.setStatus(InstallmentStatus.OVERDUE);
                    scheduleRepo.save(s);
                    totalNewPenal = totalNewPenal.add(newPenal);
                }
            }
        }

        // Post consolidated penal charge
        if (totalNewPenal.compareTo(BigDecimal.ZERO) > 0 && finePerDay.compareTo(BigDecimal.ZERO) > 0) {
            chargeService.postPenalCharge(loan, maxDpd, finePerDay);

            // Recalculate next EMI to include accumulated penalties
            recalculateNextEMI(loan, totalNewPenal);

            // Notify applicant
            if (loan.getApplication() != null && loan.getApplication().getCreatedBy() != null) {
                String email = loan.getApplication().getCreatedBy().getEmail();
                String name  = loan.getApplication().getCreatedBy().getFullName();
                notificationService.notifyDelinquencyAlert(email, name, maxDpd,
                        totalNewPenal.toPlainString());
            }

            log.info("[PENAL-SCHEDULER] Loan #{} — {} DPD, ₹{} penal charged, next EMI updated",
                    loan.getLoanAccountId(), maxDpd, totalNewPenal.toPlainString());
        }

        // Always refresh DPD record
        dpdService.computeAndUpsert(loan.getLoanAccountId());
    }

    /**
     * Adds the penal amount to the next DUE installment's totalDue and balanceDue.
     * This ensures the applicant sees the revised schedule with penalties included.
     */
    @Transactional
    public void recalculateNextEMI(LoanAccount loan, BigDecimal penalToAdd) {
        List<RepaymentSchedule> schedules =
                scheduleRepo.findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(
                        loan.getLoanAccountId());

        // Find the next DUE (not yet overdue) installment
        RepaymentSchedule nextDue = schedules.stream()
                .filter(s -> s.getStatus() == InstallmentStatus.DUE
                          && (s.getDueDate() == null || !s.getDueDate().isBefore(LocalDate.now())))
                .findFirst()
                .orElse(null);

        if (nextDue == null) {
            // All remaining installments are either PAID or OVERDUE — add to last OVERDUE
            nextDue = schedules.stream()
                    .filter(s -> s.getStatus() == InstallmentStatus.OVERDUE)
                    .reduce((a, b) -> b)  // last overdue
                    .orElse(null);
        }

        if (nextDue == null) return;

        BigDecimal newTotal   = nextDue.getTotalDue().add(penalToAdd).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalance = nextDue.getBalanceDue().add(penalToAdd).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newPenal   = (nextDue.getPenalAmount() != null ? nextDue.getPenalAmount() : BigDecimal.ZERO)
                .add(penalToAdd).setScale(2, RoundingMode.HALF_UP);

        nextDue.setTotalDue(newTotal);
        nextDue.setBalanceDue(newBalance);
        nextDue.setPenalAmount(newPenal);
        scheduleRepo.save(nextDue);

        log.info("[PENAL-SCHEDULER] EMI #{} updated: totalDue=₹{} (includes ₹{} penal)",
                nextDue.getInstallmentNo(), newTotal.toPlainString(), penalToAdd.toPlainString());
    }
}
