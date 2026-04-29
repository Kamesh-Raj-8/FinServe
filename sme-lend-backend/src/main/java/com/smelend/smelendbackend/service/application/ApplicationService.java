package com.smelend.smelendbackend.service.application;

import com.smelend.smelendbackend.dto.application.ApplicationResponse;
import com.smelend.smelendbackend.dto.application.CreateApplicationRequest;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.ApplicationStatus;
import com.smelend.smelendbackend.entity.enums.AuditAction;
import com.smelend.smelendbackend.entity.enums.KycStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.repository.KycRecordRepository;
import com.smelend.smelendbackend.service.kyc.KycVerificationService;
import com.smelend.smelendbackend.repository.LoanProductRepository;
import com.smelend.smelendbackend.repository.SmeRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import org.springframework.http.HttpStatus;
import com.smelend.smelendbackend.service.notification.NotificationService;
import com.smelend.smelendbackend.service.operations.OfferService;
import com.smelend.smelendbackend.service.scoring.DecisionEngine;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {

    private final LoanApplicationRepository appRepo;
    private final SmeRepository smeRepo;
    private final LoanProductRepository productRepo;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    private final NotificationService    notificationService;
    private final DecisionEngine         decisionEngine;
    private final KycRecordRepository    kycRecordRepo;
    private final KycVerificationService kycVerificationService;
    private final OfferService           offerService;

    public ApplicationService(
            LoanApplicationRepository appRepo,
            SmeRepository smeRepo,
            LoanProductRepository productRepo,
            CurrentUserService currentUserService,
            AuditLogService auditLogService,
            NotificationService notificationService,
            DecisionEngine decisionEngine,
            KycRecordRepository kycRecordRepo,
            KycVerificationService kycVerificationService,
            OfferService offerService) {
        this.appRepo                = appRepo;
        this.smeRepo                = smeRepo;
        this.productRepo            = productRepo;
        this.currentUserService     = currentUserService;
        this.auditLogService        = auditLogService;
        this.notificationService    = notificationService;
        this.decisionEngine         = decisionEngine;
        this.kycRecordRepo          = kycRecordRepo;
        this.kycVerificationService = kycVerificationService;
        this.offerService           = offerService;
    }

    public ApplicationResponse create(CreateApplicationRequest req) {
        AppUser me = currentUserService.getCurrentUser();

        Sme sme = smeRepo.findById(req.getSmeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SME not found"));

        boolean owner = sme.getCreatedBy() != null && sme.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner && !currentUserService.isAdmin(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot create application for this SME");
        }

        LoanProduct product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan product not found"));

        LoanApplication saved = appRepo.save(LoanApplication.builder()
                .sme(sme)
                .product(product)
                .requestedAmount(req.getRequestedAmount())
                .tenorMonths(req.getTenorMonths())
                .purposeNote(req.getPurposeNote())
                .status(ApplicationStatus.DRAFT)
                .createdBy(me)
                .createdDate(LocalDateTime.now())
                .build());

        notificationService.notifyLoanApplicationCreated(me.getEmail(), me.getFullName(), saved.getApplicationId());
        auditLogService.log(me, AuditAction.APPLICATION_CREATED, "APPLICATION", saved.getApplicationId(),
                "Application created for SME " + sme.getSmeId());

        return toDto(saved);
    }

    public List<ApplicationResponse> listMine() {
        AppUser me = currentUserService.getCurrentUser();
        String role = me.getRole().getRoleName().name();
        if (role.equals("AGENT") || role.equals("ADMIN")) {
            return appRepo.findAll().stream().map(this::toDto).toList();
        }
        return appRepo.findByCreatedBy_UserId(me.getUserId()).stream().map(this::toDto).toList();
    }

    public List<ApplicationResponse> listAll() {
        return appRepo.findAll().stream().map(this::toDto).toList();
    }

    public ApplicationResponse get(Long applicationId) {
        AppUser me = currentUserService.getCurrentUser();

        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        String role = me.getRole().getRoleName().name();
        boolean isPrivileged = role.equals("AGENT") || role.equals("ADMIN")
                || role.equals("UNDERWRITER") || role.equals("OPERATIONS");
        boolean owner = app.getCreatedBy() != null && app.getCreatedBy().getUserId().equals(me.getUserId());

        if (!owner && !isPrivileged) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot view this application");
        }

        return toDto(app);
    }

    public ApplicationResponse submit(Long applicationId) {
        AppUser me = currentUserService.getCurrentUser();

        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        boolean owner = app.getCreatedBy() != null && app.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner && !currentUserService.isAdmin(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot submit this application");
        }

        if (!(app.getStatus() == ApplicationStatus.DRAFT
                || app.getStatus() == ApplicationStatus.READY_TO_SUBMIT
                || app.getStatus() == ApplicationStatus.KYC_PENDING)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Application cannot be submitted from status: " + app.getStatus());
        }

        KycVerificationService.KycReadinessResult kycResult =
                kycVerificationService.checkAll(app);

        if (!kycResult.ready()) {
            String failureDetail = String.join(" | ", kycResult.failures());
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot submit: KYC verification incomplete. " + failureDetail);
        }

        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setSubmittedAt(java.time.LocalDateTime.now());
        app = appRepo.save(app);

        decisionEngine.scoreAndDecide(app.getApplicationId());
        app = appRepo.findById(app.getApplicationId()).orElse(app);

        if (app.getStatus() == ApplicationStatus.AUTO_APPROVED) {
            offerService.createAutoOffer(app);
            app = appRepo.findById(app.getApplicationId()).orElse(app);
        } else if (app.getStatus() == ApplicationStatus.SUBMITTED) {
            app.setStatus(ApplicationStatus.ROUTED_TO_UW);
            app = appRepo.save(app);
            if (app.getCreatedBy() != null) {
                notificationService.notifyRoutedToUnderwriter(
                        app.getCreatedBy().getEmail(),
                        app.getCreatedBy().getFullName(),
                        app.getApplicationId());
            }
        } else if (app.getStatus() != ApplicationStatus.OFFERED) {
            if (app.getStatus() == ApplicationStatus.SUBMITTED
                    || app.getStatus() == ApplicationStatus.ROUTED_TO_UW) {
                if (app.getStatus() == ApplicationStatus.SUBMITTED) {
                    app.setStatus(ApplicationStatus.ROUTED_TO_UW);
                    app = appRepo.save(app);
                }
                if (app.getCreatedBy() != null) {
                    notificationService.notifyRoutedToUnderwriter(
                            app.getCreatedBy().getEmail(),
                            app.getCreatedBy().getFullName(),
                            app.getApplicationId());
                }
            }
        }

        auditLogService.log(me, AuditAction.APPLICATION_SUBMITTED, "APPLICATION", app.getApplicationId(),
                "Application submitted — final status: " + app.getStatus());

        return toDto(app);
    }

    private ApplicationResponse toDto(LoanApplication a) {
        return ApplicationResponse.builder()
                .applicationId(a.getApplicationId())
                .smeId(a.getSme() != null ? a.getSme().getSmeId() : null)
                .smeLegalName(a.getSme() != null ? a.getSme().getLegalName() : null)
                .productId(a.getProduct() != null ? a.getProduct().getProductId() : null)
                .productName(a.getProduct() != null ? a.getProduct().getProductName() : null)
                .requestedAmount(a.getRequestedAmount())
                .tenorMonths(a.getTenorMonths())
                .purposeNote(a.getPurposeNote())
                .status(a.getStatus())
                .createdByUserId(a.getCreatedBy() != null ? a.getCreatedBy().getUserId() : null)
                .createdByEmail(a.getCreatedBy() != null ? a.getCreatedBy().getEmail() : null)
                .createdDate(a.getCreatedDate())
                .submittedAt(a.getSubmittedAt())
                .build();
    }
    public ApplicationResponse getForOperations(Long applicationId) {

        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        return toDto(app);
    }

    public List<ApplicationResponse> listApprovedForOperations() {

        return appRepo.findAll().stream()
                .filter(a -> a.getStatus() == ApplicationStatus.UW_APPROVED)
                .map(this::toDto)
                .toList();
    }
}