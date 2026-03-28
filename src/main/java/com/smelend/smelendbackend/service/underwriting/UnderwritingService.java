package com.smelend.smelendbackend.service.underwriting;

import com.smelend.smelendbackend.dto.application.ApplicationResponse;
import com.smelend.smelendbackend.dto.underwriting.UwDecisionRequest;
import com.smelend.smelendbackend.dto.underwriting.UwReviewResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.LoanApplication;
import com.smelend.smelendbackend.entity.UwReview;
import com.smelend.smelendbackend.entity.enums.ApplicationStatus;
import com.smelend.smelendbackend.entity.enums.AuditAction;
import com.smelend.smelendbackend.entity.enums.UwDecision;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.repository.UwReviewRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UnderwritingService {

    private final LoanApplicationRepository appRepo;
    private final UwReviewRepository uwRepo;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public UnderwritingService(LoanApplicationRepository appRepo,
                               UwReviewRepository uwRepo,
                               CurrentUserService currentUserService,
                               AuditLogService auditLogService) {
        this.appRepo = appRepo;
        this.uwRepo = uwRepo;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public List<ApplicationResponse> queue() {
        // Only UW/ADMIN should call (controller restricts)
        return appRepo.findByStatus(ApplicationStatus.ROUTED_TO_UW).stream()
                .map(this::toAppDto)
                .toList();
    }

    public UwReviewResponse decide(Long applicationId, UwDecisionRequest req) {
        AppUser me = currentUserService.getCurrentUser();
        requireUnderwriterOrAdmin(me);

        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (app.getStatus() != ApplicationStatus.ROUTED_TO_UW) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Application is not in underwriting queue");
        }

        UwDecision decision = req.getDecision();

        // update application status based on UW decision
        ApplicationStatus newStatus;
        if (decision == UwDecision.APPROVE) {
            newStatus = ApplicationStatus.UW_APPROVED;
        } else if (decision == UwDecision.REJECT) {
            newStatus = ApplicationStatus.UW_REJECTED;
        } else {
            // RETURN: send back to KYC_PENDING (or DRAFT) for fixes
            newStatus = ApplicationStatus.KYC_PENDING;
        }

        app.setStatus(newStatus);
        appRepo.save(app);

        // One review per application (you have unique constraint in entity)
        UwReview review = uwRepo.findByApplication_ApplicationId(applicationId).orElse(null);

        if (review == null) {
            review = UwReview.builder()
                    .application(app)
                    .underwriter(me)
                    .decision(decision)
                    .summaryNote(req.getSummaryNote())
                    .createdDate(LocalDateTime.now())
                    .build();
        } else {
            review.setUnderwriter(me);
            review.setDecision(decision);
            review.setSummaryNote(req.getSummaryNote());
            review.setCreatedDate(LocalDateTime.now());
        }

        UwReview saved = uwRepo.save(review);

        auditLogService.log(me, AuditAction.UW_DECISION, "APPLICATION", applicationId,
                "UW decision: " + decision + " -> " + newStatus);

        return UwReviewResponse.builder()
                .reviewId(saved.getReviewId())
                .applicationId(applicationId)
                .decision(saved.getDecision())
                .summaryNote(saved.getSummaryNote())
                .underwriterUserId(me.getUserId())
                .underwriterEmail(me.getEmail())
                .createdDate(saved.getCreatedDate())
                .newApplicationStatus(newStatus)
                .build();
    }

    private void requireUnderwriterOrAdmin(AppUser me) {
        String role = me.getRole().getRoleName().name();
        if (!(role.equals("UNDERWRITER") || role.equals("ADMIN"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only UNDERWRITER/ADMIN can take decisions");
        }
    }

    private ApplicationResponse toAppDto(LoanApplication a) {
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
}