package com.smelend.smelendbackend.service.operations;

import com.smelend.smelendbackend.dto.operations.disbursement.DisburseRequest;
import com.smelend.smelendbackend.dto.operations.disbursement.DisbursementResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.LoanAccountResponse;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.*;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.*;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import com.smelend.smelendbackend.service.servicing.EmiScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DisbursementService {

    private final DisbursementRepository disbRepo;
    private final LoanAccountRepository loanRepo;
    private final LoanApplicationRepository appRepo;
    private final OfferRepository offerRepo;
    private final CurrentUserService currentUserService;
    private final EmiScheduleService emiScheduleService;
    private final AuditLogService auditLogService;

    public DisbursementService(DisbursementRepository disbRepo,
                               LoanAccountRepository loanRepo,
                               LoanApplicationRepository appRepo,
                               OfferRepository offerRepo,
                               CurrentUserService currentUserService,
                               EmiScheduleService emiScheduleService,
                               AuditLogService auditLogService) {
        this.disbRepo = disbRepo;
        this.loanRepo = loanRepo;
        this.appRepo = appRepo;
        this.offerRepo = offerRepo;
        this.currentUserService = currentUserService;
        this.emiScheduleService = emiScheduleService;
        this.auditLogService = auditLogService;
    }

    /**
     * OPS disburses only if application is OFFER_ACCEPTED.
     * Creates Disbursement + LoanAccount + EMI schedule.
     */
    public DisbursementResponse disburse(Long applicationId, DisburseRequest req) {
        AppUser me = currentUserService.getCurrentUser();
        requireOpsOrAdmin(me);

        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (app.getStatus() != ApplicationStatus.OFFER_ACCEPTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Disbursement allowed only after OFFER_ACCEPTED");
        }

        Offer offer = offerRepo.findByApplication_ApplicationId(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Offer not found for application"));

        // prevent duplicates
        disbRepo.findByApplication_ApplicationId(applicationId).ifPresent(x -> {
            throw new ApiException(HttpStatus.CONFLICT, "Disbursement already exists for this application");
        });

        loanRepo.findByApplication_ApplicationId(applicationId).ifPresent(x -> {
            throw new ApiException(HttpStatus.CONFLICT, "Loan account already exists for this application");
        });

        BigDecimal amount = offer.getSanctionedAmount();
        LocalDate startDate = req.getDisbursementDate();

        Disbursement disb = disbRepo.save(Disbursement.builder()
                .application(app)
                .amount(amount)
                .mode(req.getMode())
                .transactionRef(req.getTransactionRef())
                .disbursementDate(req.getDisbursementDate())
                .status(DisbursementStatus.POSTED)
                .build());

        LoanAccount loan = loanRepo.save(LoanAccount.builder()
                .application(app)
                .accountNumber(generateAccountNumber())
                .principalSanctioned(amount)
                .interestRate(offer.getInterestRate())
                .tenorMonths(app.getTenorMonths())
                .startDate(startDate)
                .status(LoanAccountStatus.ACTIVE)
                .build());

        // Generate EMI schedule if not exists
        emiScheduleService.generateIfNotExists(loan.getLoanAccountId());

        auditLogService.log(me, AuditAction.DISBURSED, "APPLICATION", applicationId, "Disbursed amount: " + amount);
        auditLogService.log(me, AuditAction.EMI_SCHEDULE_GENERATED, "LOAN_ACCOUNT", loan.getLoanAccountId(), "EMI schedule generated");

        // update application status to DISBURSED
        app.setStatus(ApplicationStatus.DISBURSED);
        appRepo.save(app);

        return DisbursementResponse.builder()
                .disbursementId(disb.getDisbursementId())
                .applicationId(applicationId)
                .amount(disb.getAmount())
                .mode(disb.getMode())
                .transactionRef(disb.getTransactionRef())
                .disbursementDate(disb.getDisbursementDate())
                .status(disb.getStatus())
                .loanAccount(toLoanDto(loan))
                .build();
    }

    public LoanAccountResponse getLoanAccount(Long loanAccountId) {
        LoanAccount loan = loanRepo.findById(loanAccountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan account not found"));
        return toLoanDto(loan);
    }

    private void requireOpsOrAdmin(AppUser me) {
        String role = me.getRole().getRoleName().name();
        if (!(role.equals("OPERATIONS") || role.equals("ADMIN"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only OPERATIONS/ADMIN can disburse");
        }
    }

    private String generateAccountNumber() {
        // Simple unique-ish number: LA + 12 digits
        long n = ThreadLocalRandom.current().nextLong(100000000000L, 999999999999L);
        return "LA" + n;
    }

    private LoanAccountResponse toLoanDto(LoanAccount l) {
        return LoanAccountResponse.builder()
                .loanAccountId(l.getLoanAccountId())
                .applicationId(l.getApplication() != null ? l.getApplication().getApplicationId() : null)
                .accountNumber(l.getAccountNumber())
                .principalSanctioned(l.getPrincipalSanctioned())
                .interestRate(l.getInterestRate())
                .tenorMonths(l.getTenorMonths())
                .startDate(l.getStartDate())
                .status(l.getStatus())
                .build();
    }
}