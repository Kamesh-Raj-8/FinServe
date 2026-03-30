package com.smelend.smelendbackend.service.servicing;

import com.smelend.smelendbackend.dto.servicing.schedule.ScheduleResponse;
import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.entity.RepaymentSchedule;
import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import com.smelend.smelendbackend.repository.RepaymentScheduleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmiScheduleService {

    private final LoanAccountRepository loanRepo;
    private final RepaymentScheduleRepository scheduleRepo;

    public EmiScheduleService(LoanAccountRepository loanRepo, RepaymentScheduleRepository scheduleRepo) {
        this.loanRepo = loanRepo;
        this.scheduleRepo = scheduleRepo;
    }

    public List<ScheduleResponse> generateIfNotExists(Long loanAccountId) {
        LoanAccount loan = loanRepo.findById(loanAccountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan account not found"));

        List<RepaymentSchedule> existing =
                scheduleRepo.findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(loanAccountId);

        if (!existing.isEmpty()) {
            return existing.stream().map(this::toDto).toList();
        }

        BigDecimal principal = loan.getPrincipalSanctioned();
        int n = loan.getTenorMonths();

        BigDecimal annualRate = loan.getInterestRate();
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        BigDecimal principalPerMonth = principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);

        List<RepaymentSchedule> rows = new ArrayList<>();
        BigDecimal outstanding = principal;
        LocalDate due = loan.getStartDate().plusMonths(1);

        for (int i = 1; i <= n; i++) {
            BigDecimal interest = outstanding.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = principalPerMonth.add(interest).setScale(2, RoundingMode.HALF_UP);

            RepaymentSchedule s = RepaymentSchedule.builder()
                    .loanAccount(loan)
                    .installmentNo(i)
                    .dueDate(due)
                    .principalDue(principalPerMonth)
                    .interestDue(interest)
                    .totalDue(total)
                    .amountPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .balanceDue(total)
                    .status(InstallmentStatus.DUE)
                    .build();

            rows.add(s);

            outstanding = outstanding.subtract(principalPerMonth).max(BigDecimal.ZERO);
            due = due.plusMonths(1);
        }

        scheduleRepo.saveAll(rows);

        return rows.stream().map(this::toDto).toList();
    }

    public List<ScheduleResponse> list(Long loanAccountId) {
        loanRepo.findById(loanAccountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan account not found"));

        return scheduleRepo.findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(loanAccountId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ScheduleResponse toDto(RepaymentSchedule r) {
        return ScheduleResponse.builder()
                .scheduleId(r.getScheduleId())
                .loanAccountId(r.getLoanAccount() != null ? r.getLoanAccount().getLoanAccountId() : null)
                .installmentNo(r.getInstallmentNo())
                .dueDate(r.getDueDate())
                .principalDue(r.getPrincipalDue())
                .interestDue(r.getInterestDue())
                .totalDue(r.getTotalDue())
                .amountPaid(r.getAmountPaid())
                .balanceDue(r.getBalanceDue())
                .status(r.getStatus())
                .build();
    }
}