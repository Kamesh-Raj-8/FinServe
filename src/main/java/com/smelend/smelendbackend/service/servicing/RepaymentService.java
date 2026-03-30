package com.smelend.smelendbackend.service.servicing;

import com.smelend.smelendbackend.dto.servicing.repayment.PostRepaymentRequest;
import com.smelend.smelendbackend.dto.servicing.repayment.RepaymentResponse;
import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.entity.Repayment;
import com.smelend.smelendbackend.entity.RepaymentSchedule;
import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import com.smelend.smelendbackend.repository.RepaymentRepository;
import com.smelend.smelendbackend.repository.RepaymentScheduleRepository;
import com.smelend.smelendbackend.service.collections.DpdService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RepaymentService {

    private final LoanAccountRepository loanRepo;
    private final RepaymentRepository repayRepo;
    private final RepaymentScheduleRepository scheduleRepo;
    private final DpdService dpdService;

    public RepaymentService(LoanAccountRepository loanRepo,
                            RepaymentRepository repayRepo,
                            RepaymentScheduleRepository scheduleRepo,
                            DpdService dpdService) {
        this.loanRepo = loanRepo;
        this.repayRepo = repayRepo;
        this.scheduleRepo = scheduleRepo;
        this.dpdService = dpdService;
    }

    public RepaymentResponse post(PostRepaymentRequest req) {
        LoanAccount loan = loanRepo.findById(req.getLoanAccountId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan account not found"));

        Repayment saved = repayRepo.save(Repayment.builder()
                .loanAccount(loan)
                .amount(req.getAmount())
                .mode(req.getMode())
                .referenceNo(req.getReferenceNo())
                .paymentDate(req.getPaymentDate())
                .createdDate(LocalDateTime.now())
                .build());

        allocateToSchedule(loan.getLoanAccountId(), req.getAmount(), req.getPaymentDate());

        dpdService.computeAndUpsert(loan.getLoanAccountId());

        return toDto(saved);
    }

    public List<RepaymentResponse> list(Long loanAccountId) {
        loanRepo.findById(loanAccountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan account not found"));

        return repayRepo.findByLoanAccount_LoanAccountIdOrderByPaymentDateDesc(loanAccountId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void allocateToSchedule(Long loanAccountId, BigDecimal amount, LocalDate paymentDate) {
        BigDecimal remaining = amount.setScale(2, RoundingMode.HALF_UP);

        List<RepaymentSchedule> schedules =
                scheduleRepo.findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(loanAccountId);

        for (RepaymentSchedule s : schedules) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal currentBalance = s.getBalanceDue() == null
                    ? (s.getTotalDue() == null ? BigDecimal.ZERO : s.getTotalDue())
                    : s.getBalanceDue();

            if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
                s.setAmountPaid(s.getTotalDue());
                s.setBalanceDue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                s.setStatus(InstallmentStatus.PAID);
                scheduleRepo.save(s);
                continue;
            }

            BigDecimal allocation = remaining.min(currentBalance).setScale(2, RoundingMode.HALF_UP);

            BigDecimal alreadyPaid = s.getAmountPaid() == null
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : s.getAmountPaid();

            BigDecimal newPaid = alreadyPaid.add(allocation).setScale(2, RoundingMode.HALF_UP);
            BigDecimal newBalance = currentBalance.subtract(allocation).setScale(2, RoundingMode.HALF_UP);

            s.setAmountPaid(newPaid);
            s.setBalanceDue(newBalance);

            if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
                s.setBalanceDue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                s.setStatus(InstallmentStatus.PAID);
            } else {
                if (s.getDueDate() != null && paymentDate.isAfter(s.getDueDate())) {
                    s.setStatus(InstallmentStatus.OVERDUE);
                } else {
                    s.setStatus(InstallmentStatus.DUE);
                }
            }

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