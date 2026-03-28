package com.smelend.smelendbackend.service.application;

import com.smelend.smelendbackend.dto.application.ApplicationResponse;
import com.smelend.smelendbackend.dto.application.CreateApplicationRequest;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.ApplicationStatus;
import com.smelend.smelendbackend.entity.enums.AuditAction;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.repository.LoanProductRepository;
import com.smelend.smelendbackend.repository.SmeRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import org.springframework.http.HttpStatus;
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

    public ApplicationService(
            LoanApplicationRepository appRepo,
            SmeRepository smeRepo,
            LoanProductRepository productRepo,
            CurrentUserService currentUserService,
            AuditLogService auditLogService
    ) {
        this.appRepo = appRepo;
        this.smeRepo = smeRepo;
        this.productRepo = productRepo;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public ApplicationResponse create(CreateApplicationRequest req) {
        AppUser me = currentUserService.getCurrentUser();

        Sme sme = smeRepo.findById(req.getSmeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SME not found"));

        // only SME owner (or ADMIN) can create application for SME
        boolean owner = sme.getCreatedBy() != null && sme.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner) {
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

        auditLogService.log(me, AuditAction.APPLICATION_CREATED, "APPLICATION", saved.getApplicationId(),
                "Application created for SME " + sme.getSmeId());

        return toDto(saved);
    }

    public List<ApplicationResponse> listMine() {
        AppUser me = currentUserService.getCurrentUser();
        return appRepo.findByCreatedBy_UserId(me.getUserId()).stream().map(this::toDto).toList();
    }

    public ApplicationResponse get(Long applicationId) {
        AppUser me = currentUserService.getCurrentUser();

        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        boolean owner = app.getCreatedBy() != null && app.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot view this application");
        }

        return toDto(app);
    }

    /**
     * Submit flow for Phase-1:
     * DRAFT (or READY_TO_SUBMIT later after KYC) -> SUBMITTED -> ROUTED_TO_UW
     */
    public ApplicationResponse submit(Long applicationId) {
        AppUser me = currentUserService.getCurrentUser();

        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        boolean owner = app.getCreatedBy() != null && app.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot submit this application");
        }

        if (!(app.getStatus() == ApplicationStatus.DRAFT || app.getStatus() == ApplicationStatus.READY_TO_SUBMIT)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Application cannot be submitted from status: " + app.getStatus());
        }

        // Phase-1 routing
        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setStatus(ApplicationStatus.ROUTED_TO_UW);

        app = appRepo.save(app);

        auditLogService.log(me, AuditAction.APPLICATION_SUBMITTED, "APPLICATION", app.getApplicationId(),
                "Application submitted and routed to UW");

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