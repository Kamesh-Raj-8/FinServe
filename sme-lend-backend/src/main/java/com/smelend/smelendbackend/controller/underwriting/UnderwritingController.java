package com.smelend.smelendbackend.controller.underwriting;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.application.ApplicationResponse;
import com.smelend.smelendbackend.dto.document.DocumentResponse;
import com.smelend.smelendbackend.dto.kyc.KycResponse;
import com.smelend.smelendbackend.dto.onboarding.promoter.PromoterResponse;
import com.smelend.smelendbackend.dto.onboarding.sme.SmeResponse;
import com.smelend.smelendbackend.dto.scoring.DecisionResponse;
import com.smelend.smelendbackend.dto.scoring.ScorecardResponse;
import com.smelend.smelendbackend.dto.underwriting.UwDecisionRequest;
import com.smelend.smelendbackend.dto.underwriting.UwReviewResponse;
import com.smelend.smelendbackend.service.application.ApplicationService;
import com.smelend.smelendbackend.service.application.DocumentService;
import com.smelend.smelendbackend.service.kyc.KycService;
import com.smelend.smelendbackend.service.onboarding.PromoterService;
import com.smelend.smelendbackend.service.onboarding.SmeService;
import com.smelend.smelendbackend.service.scoring.DecisionEngine;
import com.smelend.smelendbackend.service.underwriting.UnderwritingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Underwriting module controller.
 * Combines UW queue/decisions AND scoring reads (scoring supports UW work).
 */
@RestController
@RequestMapping("/uw")
public class UnderwritingController {

    private final UnderwritingService underwritingService;
    private final DecisionEngine      decisionEngine;
    private final ApplicationService  applicationService;
    private final SmeService          smeService;
    private final PromoterService     promoterService;
    private final KycService          kycService;
    private final DocumentService     documentService;

    public UnderwritingController(UnderwritingService underwritingService,
                                  DecisionEngine decisionEngine,
                                  ApplicationService applicationService,
                                  SmeService smeService,
                                  PromoterService promoterService,
                                  KycService kycService,
                                  DocumentService documentService) {
        this.underwritingService = underwritingService;
        this.decisionEngine      = decisionEngine;
        this.applicationService  = applicationService;
        this.smeService          = smeService;
        this.promoterService     = promoterService;
        this.kycService          = kycService;
        this.documentService     = documentService;
    }

    // ── UW DECISION (UNDERWRITER only) ────────────────────────────────

    @PostMapping("/applications/{applicationId}/decision")
    @PreAuthorize("hasAnyRole('UNDERWRITER')")
    public ApiResponse<UwReviewResponse> decide(
            @PathVariable Long applicationId,
            @Valid @RequestBody UwDecisionRequest req) {
        return ApiResponse.ok("Decision recorded",
                underwritingService.decide(applicationId, req));
    }

    // ── SCORING — reads served from UW module (UNDERWRITER + ADMIN) ───

    @GetMapping("/applications/{applicationId}/scorecard")
    @PreAuthorize("hasAnyRole('UNDERWRITER','ADMIN','OPERATIONS','AGENT')")
    public ApiResponse<ScorecardResponse> getScorecard(@PathVariable Long applicationId) {
        return ApiResponse.ok("Scorecard fetched",
                decisionEngine.getScorecardByApplication(applicationId));
    }

    @GetMapping("/applications/{applicationId}/decision-result")
    @PreAuthorize("hasAnyRole('UNDERWRITER','ADMIN','OPERATIONS','AGENT')")
    public ApiResponse<DecisionResponse> getDecision(@PathVariable Long applicationId) {
        return ApiResponse.ok("Decision fetched",
                decisionEngine.getDecisionByApplication(applicationId));
    }

    @PostMapping("/applications/{applicationId}/rescore")
    @PreAuthorize("hasAnyRole('UNDERWRITER')")
    public ApiResponse<DecisionResponse> rescore(@PathVariable Long applicationId) {
        return ApiResponse.ok("Rescoring completed",
                decisionEngine.scoreAndDecide(applicationId));
    }

    // ── UW QUEUE & CROSS-REFERENCE READS ─────────────────────────────

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('UNDERWRITER','ADMIN')")
    public ApiResponse<List<ApplicationResponse>> queue() {
        return ApiResponse.ok("UW queue fetched", underwritingService.queue());
    }

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasAnyRole('UNDERWRITER','ADMIN')")
    public ApiResponse<ApplicationResponse> getApplication(@PathVariable Long applicationId) {
        return ApiResponse.ok("Application fetched", applicationService.get(applicationId));
    }

    @GetMapping("/applications/{applicationId}/sme")
    @PreAuthorize("hasAnyRole('UNDERWRITER','ADMIN')")
    public ApiResponse<SmeResponse> getSme(@PathVariable Long applicationId) {
        ApplicationResponse app = applicationService.get(applicationId);
        return ApiResponse.ok("SME fetched", smeService.get(app.getSmeId()));
    }

    @GetMapping("/applications/{applicationId}/kyc")
    @PreAuthorize("hasAnyRole('UNDERWRITER','ADMIN')")
    public ApiResponse<List<KycResponse>> getKyc(@PathVariable Long applicationId) {
        ApplicationResponse app = applicationService.get(applicationId);
        return ApiResponse.ok("KYC records fetched", kycService.listBySme(app.getSmeId()));
    }

    @GetMapping("/applications/{applicationId}/promoters")
    @PreAuthorize("hasAnyRole('UNDERWRITER','ADMIN')")
    public ApiResponse<List<PromoterResponse>> getPromoters(@PathVariable Long applicationId) {
        ApplicationResponse app = applicationService.get(applicationId);
        return ApiResponse.ok("Promoters fetched", promoterService.listBySme(app.getSmeId()));
    }

    @GetMapping("/applications/{applicationId}/documents")
    @PreAuthorize("hasAnyRole('UNDERWRITER','ADMIN')")
    public ApiResponse<List<DocumentResponse>> getDocs(@PathVariable Long applicationId) {
        return ApiResponse.ok("Documents fetched", documentService.list(applicationId));
    }
}
