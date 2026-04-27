package com.smelend.smelendbackend.service.collections;

import com.smelend.smelendbackend.entity.Delinquency;
import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.entity.RepaymentSchedule;
import com.smelend.smelendbackend.entity.enums.BucketType;
import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.DelinquencyRepository;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import com.smelend.smelendbackend.repository.RepaymentScheduleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class DpdService {

    private final LoanAccountRepository loanRepo;
    private final RepaymentScheduleRepository scheduleRepo;
    private final DelinquencyRepository delinRepo;

    public DpdService(LoanAccountRepository loanRepo,
                      RepaymentScheduleRepository scheduleRepo,
                      DelinquencyRepository delinRepo) {
        this.loanRepo = loanRepo;
        this.scheduleRepo = scheduleRepo;
        this.delinRepo = delinRepo;
    }

    public Delinquency computeAndUpsert(Long loanAccountId) {
        LoanAccount loan = loanRepo.findById(loanAccountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan account not found"));

        LocalDate today = LocalDate.now();

        List<RepaymentSchedule> schedules =
                scheduleRepo.findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(loanAccountId);

        // DPD = max days late among unpaid installments
        int dpd = 0;
        for (RepaymentSchedule s : schedules) {
            if (s.getStatus() != InstallmentStatus.PAID && s.getDueDate() != null && s.getDueDate().isBefore(today)) {
                int days = (int) ChronoUnit.DAYS.between(s.getDueDate(), today);
                dpd = Math.max(dpd, days);
            }
        }

        BucketType bucket = bucketFromDpd(dpd);

        Delinquency del = delinRepo.findByLoanAccount_LoanAccountId(loanAccountId).orElse(null);
        if (del == null) {
            del = Delinquency.builder()
                    .loanAccount(loan)
                    .dpd(dpd)
                    .bucket(bucket)
                    .asOfDate(today)
                    .updatedDate(LocalDateTime.now())
                    .build();
        } else {
            del.setDpd(dpd);
            del.setBucket(bucket);
            del.setAsOfDate(today);
            del.setUpdatedDate(LocalDateTime.now());
        }

        return delinRepo.save(del);
    }

    private BucketType bucketFromDpd(int dpd) {
        if (dpd <= 0) return BucketType.CURRENT;
        if (dpd <= 30) return BucketType.DPD_1_30;
        if (dpd <= 60) return BucketType.DPD_31_60;
        if (dpd <= 90) return BucketType.DPD_61_90;
        return BucketType.DPD_90_PLUS;
    }
}