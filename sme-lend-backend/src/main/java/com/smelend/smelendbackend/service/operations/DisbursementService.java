package com.smelend.smelendbackend.service.operations;

import com.smelend.smelendbackend.dto.operations.disbursement.DisburseRequest;
import com.smelend.smelendbackend.dto.operations.disbursement.DisbursementResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.LoanAccountResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.PendingDisbursementDto;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.*;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.*;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import com.smelend.smelendbackend.service.servicing.EmiScheduleService;
import org.springframework.http.HttpStatus;
import com.smelend.smelendbackend.service.notification.NotificationService;
import com.smelend.smelendbackend.service.fee.FeeService;
import com.smelend.smelendbackend.util.DisbursementCalculator;
import com.smelend.smelendbackend.dto.fee.AppliedFeeDto;
import com.smelend.smelendbackend.service.charge.ChargeService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class DisbursementService {

    private final DisbursementRepository disbRepo;
    private final LoanAccountRepository loanRepo;
    private final LoanApplicationRepository appRepo;
    private final OfferRepository offerRepo;
    private final CurrentUserService currentUserService;
    private final EmiScheduleService emiScheduleService;
    private final AuditLogService auditLogService;

    private final NotificationService notificationService;
    private final FeeService           feeService;
    private final ChargeService        chargeService;

    public DisbursementService(DisbursementRepository disbRepo,
                               LoanAccountRepository loanRepo,
                               LoanApplicationRepository appRepo,
                               OfferRepository offerRepo,
                               CurrentUserService currentUserService,
                               EmiScheduleService emiScheduleService,
                               AuditLogService auditLogService,
                                  NotificationService notificationService,
                                  FeeService feeService,
                                  ChargeService chargeService) {
        this.disbRepo = disbRepo;
        this.loanRepo = loanRepo;
        this.appRepo = appRepo;
        this.offerRepo = offerRepo;
        this.currentUserService = currentUserService;
        this.emiScheduleService = emiScheduleService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.feeService          = feeService;
        this.chargeService       = chargeService;
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

        // ── Auto-calculate: sanctioned amount always comes from offer ──────
        BigDecimal sanctionedAmount = offer.getSanctionedAmount();
        LocalDate startDate = req.getDisbursementDate();

        // Calculate applicable upfront fees (PROCESSING, LEGAL, INSURANCE, TECH)
        java.util.List<AppliedFeeDto> appliedFees = feeService.calculateFees(
                app.getProduct().getProductId(), sanctionedAmount);

        // Net amount = sanctioned - upfront fees (PENAL excluded from upfront)
        BigDecimal netAmount = DisbursementCalculator.netDisbursedAmount(sanctionedAmount, appliedFees);

        Disbursement disb = disbRepo.save(Disbursement.builder()
                .application(app)
                .amount(netAmount)
                .mode(req.getMode())
                .transactionRef(req.getTransactionRef())
                .disbursementDate(req.getDisbursementDate())
                .status(DisbursementStatus.POSTED)
                .build());

        LoanAccount loan = loanRepo.save(LoanAccount.builder()
                .application(app)
                .accountNumber(generateAccountNumber(app, app.getProduct() != null ? app.getProduct().getProductId() : null))
                .principalSanctioned(sanctionedAmount)  // schedule based on gross sanctioned
                .interestRate(offer.getInterestRate())
                .tenorMonths(app.getTenorMonths())
                .startDate(startDate)
                .status(LoanAccountStatus.ACTIVE)
                .build());

        // Generate EMI schedule based on gross sanctioned amount
        emiScheduleService.generateIfNotExists(loan.getLoanAccountId());

        // Post processing fee charges as individual Charge records
        for (AppliedFeeDto fee : appliedFees) {
            if (!com.smelend.smelendbackend.entity.enums.FeeType.PENAL.name().equals(fee.getFeeType())
                    && fee.getCalculatedAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                chargeService.postProcessingFee(loan, fee.getCalculatedAmount());
            }
        }
        if (app.getCreatedBy() != null) {
            notificationService.notifyLoanDisbursed(
                app.getCreatedBy().getEmail(),
                app.getCreatedBy().getFullName(),
                loan.getLoanAccountId(),
                netAmount.toPlainString());
        }

        auditLogService.log(me, AuditAction.DISBURSED, "APPLICATION", applicationId, "Net disbursed: " + netAmount + " / Sanctioned: " + sanctionedAmount);
        auditLogService.log(me, AuditAction.EMI_SCHEDULE_GENERATED, "LOAN_ACCOUNT", loan.getLoanAccountId(), "EMI schedule generated");

        // update application status to DISBURSED
        app.setStatus(ApplicationStatus.DISBURSED);
        appRepo.save(app);

        return DisbursementResponse.builder()
                .disbursementId(disb.getDisbursementId())
                .applicationId(applicationId)
                .amount(netAmount)         // net disbursed
                .sanctionedAmount(sanctionedAmount)  // gross sanctioned
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

    /**
     * Returns all applications in OFFER_ACCEPTED status with their offer details.
     * Used by Operations to see exactly what is ready to disburse.
     */
    public java.util.List<PendingDisbursementDto> listPendingDisbursements() {
        return offerRepo.findAllForAcceptedApplications().stream()
                .map(offer -> {
                    var app = offer.getApplication();
                    return PendingDisbursementDto.builder()
                            .applicationId(app.getApplicationId())
                            .smeLegalName(app.getSme() != null ? app.getSme().getLegalName() : "—")
                            .smeId(app.getSme() != null ? app.getSme().getSmeId() : null)
                            .productName(app.getProduct() != null ? app.getProduct().getProductName() : "—")
                            .applicantEmail(app.getCreatedBy() != null ? app.getCreatedBy().getEmail() : "—")
                            .applicationStatus(app.getStatus().name())
                            .submittedAt(app.getSubmittedAt())
                            .offerId(offer.getOfferId())
                            .sanctionedAmount(offer.getSanctionedAmount())
                            .interestRate(offer.getInterestRate())
                            .emiAmount(offer.getEmiAmount())
                            .offerValidUntil(offer.getValidUntil() != null ? offer.getValidUntil().toString() : null)
                            .offerStatus(offer.getOfferStatus().name())
                            .offerCreatedAt(offer.getCreatedDate())
                            .build();
                })
                .toList();
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

    /**
     * Loan account number = LA-{productId:04d}-{bankAccountNo}.
     * Deterministic: same applicant + product always yields the same format.
     */
    private String generateAccountNumber(com.smelend.smelendbackend.entity.LoanApplication app,
                                          Long productId) {
        String bankAccNo = (app.getCreatedBy() != null
                && app.getCreatedBy().getBankAccountNo() != null
                && !app.getCreatedBy().getBankAccountNo().isBlank())
                ? app.getCreatedBy().getBankAccountNo()
                : String.valueOf(app.getApplicationId());
        return String.format("LA-%04d-%s", productId != null ? productId : 0, bankAccNo);
    }
}
