package com.smelend.smelendbackend.service.servicing;

import com.smelend.smelendbackend.dto.servicing.repayment.PostRepaymentRequest;
import com.smelend.smelendbackend.dto.servicing.repayment.RepaymentResponse;
import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.entity.Repayment;
import com.smelend.smelendbackend.entity.RepaymentSchedule;
import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import com.smelend.smelendbackend.entity.enums.LoanAccountStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import com.smelend.smelendbackend.repository.RepaymentRepository;
import com.smelend.smelendbackend.repository.RepaymentScheduleRepository;
import com.smelend.smelendbackend.service.collections.DpdService;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RepaymentService {

    private final LoanAccountRepository          loanRepo;
    private final RepaymentRepository            repayRepo;
    private final RepaymentScheduleRepository    scheduleRepo;
    private final DpdService                     dpdService;
    private final NotificationService            notificationService;
    private final PenalSchedulingService         penalSchedulingService;
    private final CurrentUserService             currentUserService;

    public RepaymentService(LoanAccountRepository loanRepo,
                            RepaymentRepository repayRepo,
                            RepaymentScheduleRepository scheduleRepo,
                            DpdService dpdService,
                            NotificationService notificationService,
                            PenalSchedulingService penalSchedulingService,
                            CurrentUserService currentUserService) {
        this.loanRepo             = loanRepo;
        this.repayRepo            = repayRepo;
        this.scheduleRepo         = scheduleRepo;
        this.dpdService           = dpdService;
        this.notificationService  = notificationService;
        this.penalSchedulingService = penalSchedulingService;
        this.currentUserService   = currentUserService;
    }

    @Transactional
    public RepaymentResponse post(PostRepaymentRequest req) {
        // ── Service-level role gate: SERVICING or ADMIN only ─────────────────
        if (!currentUserService.hasAnyRole("SERVICING", "ADMIN")) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only SERVICING/ADMIN can post repayments");
        }

        LoanAccount loan = loanRepo.findById(req.getLoanAccountId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan account not found"));

        if (loan.getStatus() != LoanAccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Repayments can only be posted to ACTIVE loan accounts. Status: " + loan.getStatus());
        }

        // ── Resolve amount: from scheduleId (auto) or explicit field ─────────
        BigDecimal amount;
        Long targetScheduleId = null;

        if (req.getScheduleId() != null) {
            RepaymentSchedule targetSlot = scheduleRepo.findById(req.getScheduleId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "Schedule row #" + req.getScheduleId() + " not found"));

            if (!targetSlot.getLoanAccount().getLoanAccountId().equals(req.getLoanAccountId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Schedule #" + req.getScheduleId() + " does not belong to loan account #"
                        + req.getLoanAccountId());
            }
            if (targetSlot.getStatus() == InstallmentStatus.PAID) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Installment #" + targetSlot.getInstallmentNo() + " is already PAID.");
            }

            // Auto-amount = balance remaining on this specific installment
            BigDecimal balance = targetSlot.getBalanceDue() != null
                    ? targetSlot.getBalanceDue()
                    : targetSlot.getTotalDue();
            amount = balance.setScale(2, RoundingMode.HALF_UP);
            targetScheduleId = req.getScheduleId();
        } else {
            if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Either scheduleId (auto-amount) or an explicit amount > 0 must be provided.");
            }
            amount = req.getAmount().setScale(2, RoundingMode.HALF_UP);

            // ── Overpayment guard ─────────────────────────────────────────────
            BigDecimal outstandingBalance = scheduleRepo
                    .findUnpaidByLoan(loan.getLoanAccountId(), InstallmentStatus.PAID)
                    .stream()
                    .map(s -> s.getBalanceDue() != null ? s.getBalanceDue() : s.getTotalDue())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            if (amount.compareTo(outstandingBalance) > 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Payment amount ₹" + amount + " exceeds outstanding balance ₹" + outstandingBalance
                        + ". Please enter an amount ≤ the outstanding balance.");
            }
        }

        Repayment saved = repayRepo.save(Repayment.builder()
                .loanAccount(loan)
                .amount(amount)
                .mode(req.getMode())
                .referenceNo(req.getReferenceNo())
                .paymentDate(req.getPaymentDate())
                .createdDate(LocalDateTime.now())
                .build());

        // ── Allocate to schedule ──────────────────────────────────────────────
        if (targetScheduleId != null) {
            // Schedule-targeted: mark exactly this slot as PAID
            allocateToSpecificSchedule(targetScheduleId, amount, req.getPaymentDate());
        } else {
            // Undirected: waterfall allocation across all DUE slots
            allocateWaterfall(loan.getLoanAccountId(), amount, req.getPaymentDate());
        }

        dpdService.computeAndUpsert(loan.getLoanAccountId());
        penalSchedulingService.processLoan(loan, req.getPaymentDate());

        // Notify applicant
        if (loan.getApplication() != null && loan.getApplication().getCreatedBy() != null) {
            notificationService.notifyRepaymentReceived(
                    loan.getApplication().getCreatedBy().getEmail(),
                    loan.getApplication().getCreatedBy().getFullName(),
                    amount.toPlainString(),
                    saved.getReferenceNo() != null ? saved.getReferenceNo() : "#" + saved.getRepaymentId());
        }

        return toDto(saved);
    }

    public List<RepaymentResponse> list(Long loanAccountId) {
        loanRepo.findById(loanAccountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan account not found"));
        return repayRepo.findByLoanAccount_LoanAccountIdOrderByPaymentDateDesc(loanAccountId)
                .stream().map(this::toDto).toList();
    }

    // ── Private: targeted allocation (schedule-id driven) ─────────────────

    private void allocateToSpecificSchedule(Long scheduleId, BigDecimal amount, LocalDate paymentDate) {
        RepaymentSchedule s = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Schedule row not found"));

        BigDecimal balance = s.getBalanceDue() != null ? s.getBalanceDue() : s.getTotalDue();
        BigDecimal allocation = amount.min(balance).setScale(2, RoundingMode.HALF_UP);

        BigDecimal alreadyPaid = s.getAmountPaid() != null ? s.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal newPaid    = alreadyPaid.add(allocation).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalance = balance.subtract(allocation).setScale(2, RoundingMode.HALF_UP);

        s.setAmountPaid(newPaid);
        s.setBalanceDue(newBalance);
        s.setStatus(newBalance.compareTo(BigDecimal.ZERO) <= 0
                ? InstallmentStatus.PAID
                : (paymentDate != null && s.getDueDate() != null && paymentDate.isAfter(s.getDueDate())
                        ? InstallmentStatus.OVERDUE : InstallmentStatus.DUE));
        scheduleRepo.save(s);
    }

    // ── Private: waterfall allocation (no scheduleId given) ───────────────

    private void allocateWaterfall(Long loanAccountId, BigDecimal amount, LocalDate paymentDate) {
        BigDecimal remaining = amount.setScale(2, RoundingMode.HALF_UP);

        List<RepaymentSchedule> schedules =
                scheduleRepo.findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(loanAccountId);

        for (RepaymentSchedule s : schedules) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal balance = s.getBalanceDue() != null ? s.getBalanceDue()
                    : (s.getTotalDue() != null ? s.getTotalDue() : BigDecimal.ZERO);

            // Already fully paid — ensure status is PAID and move on
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                if (s.getStatus() != InstallmentStatus.PAID) {
                    s.setAmountPaid(s.getTotalDue());
                    s.setBalanceDue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    s.setStatus(InstallmentStatus.PAID);
                    scheduleRepo.save(s);
                }
                continue;
            }

            BigDecimal allocation = remaining.min(balance).setScale(2, RoundingMode.HALF_UP);
            BigDecimal alreadyPaid = s.getAmountPaid() != null ? s.getAmountPaid() : BigDecimal.ZERO;
            BigDecimal newPaid    = alreadyPaid.add(allocation).setScale(2, RoundingMode.HALF_UP);
            BigDecimal newBalance = balance.subtract(allocation).setScale(2, RoundingMode.HALF_UP);

            s.setAmountPaid(newPaid);
            s.setBalanceDue(newBalance);
            s.setStatus(newBalance.compareTo(BigDecimal.ZERO) <= 0
                    ? InstallmentStatus.PAID
                    : (paymentDate != null && s.getDueDate() != null && paymentDate.isAfter(s.getDueDate())
                            ? InstallmentStatus.OVERDUE : InstallmentStatus.DUE));
            scheduleRepo.save(s);
            remaining = remaining.subtract(allocation).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private RepaymentResponse toDto(Repayment r) {
        return RepaymentResponse.builder()
                .repaymentId(r.getRepaymentId())
                .loanAccountId(r.getLoanAccount() != null ? r.getLoanAccount().getLoanAccountId() : null)
                .amount(r.getAmount())
                .mode(r.getMode())
                .referenceNo(r.getReferenceNo())
                .paymentDate(r.getPaymentDate())
                .createdDate(r.getCreatedDate())
                .build();
    }
}
