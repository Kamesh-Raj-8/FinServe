package com.smelend.smelendbackend.service.collections;

import com.smelend.smelendbackend.dto.collections.CreatePtpRequest;
import com.smelend.smelendbackend.dto.collections.DelinquencyResponse;
import com.smelend.smelendbackend.dto.collections.PtpResponse;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import com.smelend.smelendbackend.entity.enums.PtpStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.*;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CollectionsService {

    private final LoanAccountRepository        loanRepo;
    private final DelinquencyRepository        delinRepo;
    private final PtpRepository                ptpRepo;
    private final RepaymentScheduleRepository  scheduleRepo;
    private final CurrentUserService           currentUserService;
    private final DpdService                   dpdService;
    private final NotificationService          notificationService;

    public CollectionsService(LoanAccountRepository loanRepo,
                              DelinquencyRepository delinRepo,
                              PtpRepository ptpRepo,
                              RepaymentScheduleRepository scheduleRepo,
                              CurrentUserService currentUserService,
                              DpdService dpdService,
                              NotificationService notificationService) {
        this.loanRepo            = loanRepo;
        this.delinRepo           = delinRepo;
        this.ptpRepo             = ptpRepo;
        this.scheduleRepo        = scheduleRepo;
        this.currentUserService  = currentUserService;
        this.dpdService          = dpdService;
        this.notificationService = notificationService;
    }

    public DelinquencyResponse getDelinquency(Long loanAccountId) {
        loanRepo.findById(loanAccountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan account not found"));
        Delinquency del = dpdService.computeAndUpsert(loanAccountId);
        return toDelDto(del);
    }

    /**
     * Returns all delinquency records in the system.
     *
     * Two-pass approach:
     *   Pass 1 — identify any loan with a currently overdue unpaid schedule and
     *             refresh (or create) its delinquency record via DpdService.
     *             This catches new overdue loans that the nightly scheduler hasn't
     *             processed yet.
     *   Pass 2 — return ALL rows from the delinquency table, so records already
     *             populated by the scheduler are always included in the response.
     */
    @Transactional
    public List<DelinquencyResponse> listAllDelinquencies() {
        LocalDate today = LocalDate.now();

        // Pass 1: refresh DPD for any loan that currently has an overdue unpaid installment
        scheduleRepo.findAllOverdueLoanIds(today, InstallmentStatus.PAID)
                .forEach(dpdService::computeAndUpsert);

        // Pass 2: return everything in the delinquency table
        return delinRepo.findAll()
                .stream()
                .map(this::toDelDto)
                .toList();
    }

    public PtpResponse createPtp(CreatePtpRequest req) {
        AppUser me = currentUserService.getCurrentUser();
        requireCollectionsOrAdmin(me);

        LoanAccount loan = loanRepo.findById(req.getLoanAccountId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan account not found"));

        Ptp saved = ptpRepo.save(Ptp.builder()
                .loanAccount(loan)
                .promiseDate(req.getPromiseDate())
                .promisedAmount(req.getPromisedAmount())
                .status(PtpStatus.OPEN)
                .notes(req.getNotes())
                .createdBy(me)
                .createdDate(LocalDateTime.now())
                .build());

        if (loan.getApplication() != null && loan.getApplication().getCreatedBy() != null) {
            AppUser borrower = loan.getApplication().getCreatedBy();
            notificationService.notifyPtpCreated(
                borrower.getEmail(),
                borrower.getFullName(),
                req.getPromiseDate().toString(),
                req.getPromisedAmount().toPlainString());
        }
        return toPtpDto(saved);
    }

    public List<PtpResponse> listPtps(Long loanAccountId) {
        loanRepo.findById(loanAccountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan account not found"));
        return ptpRepo.findByLoanAccount_LoanAccountIdOrderByCreatedDateDesc(loanAccountId)
                .stream().map(this::toPtpDto).toList();
    }

    public PtpResponse updatePtpStatus(Long ptpId, PtpStatus status) {
        AppUser me = currentUserService.getCurrentUser();
        requireCollectionsOrAdmin(me);

        Ptp ptp = ptpRepo.findById(ptpId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PTP not found"));
        ptp.setStatus(status);
        return toPtpDto(ptpRepo.save(ptp));
    }

    private void requireCollectionsOrAdmin(AppUser me) {
        String role = me.getRole().getRoleName().name();
        if (!(role.equals("COLLECTIONS") || role.equals("ADMIN"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only COLLECTIONS/ADMIN allowed");
        }
    }

    private DelinquencyResponse toDelDto(Delinquency d) {
        return DelinquencyResponse.builder()
                .delinquencyId(d.getDelinquencyId())
                .loanAccountId(d.getLoanAccount() != null ? d.getLoanAccount().getLoanAccountId() : null)
                .dpd(d.getDpd())
                .bucket(d.getBucket())
                .asOfDate(d.getAsOfDate())
                .updatedDate(d.getUpdatedDate())
                .build();
    }

    private PtpResponse toPtpDto(Ptp p) {
        return PtpResponse.builder()
                .ptpId(p.getPtpId())
                .loanAccountId(p.getLoanAccount() != null ? p.getLoanAccount().getLoanAccountId() : null)
                .promiseDate(p.getPromiseDate())
                .promisedAmount(p.getPromisedAmount())
                .status(p.getStatus())
                .notes(p.getNotes())
                .createdByUserId(p.getCreatedBy() != null ? p.getCreatedBy().getUserId() : null)
                .createdByEmail(p.getCreatedBy() != null ? p.getCreatedBy().getEmail() : null)
                .createdDate(p.getCreatedDate())
                .build();
    }
}