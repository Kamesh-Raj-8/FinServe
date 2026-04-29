package com.smelend.smelendbackend.controller.admin;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.application.ApplicationResponse;
import com.smelend.smelendbackend.dto.collections.DelinquencyResponse;
import com.smelend.smelendbackend.dto.collections.PtpResponse;
import com.smelend.smelendbackend.dto.compliance.AuditLogResponse;
import com.smelend.smelendbackend.dto.kyc.KycResponse;
import com.smelend.smelendbackend.dto.onboarding.promoter.PromoterResponse;
import com.smelend.smelendbackend.dto.onboarding.sme.SmeResponse;
import com.smelend.smelendbackend.dto.operations.offer.OfferResponse;
import com.smelend.smelendbackend.dto.risk.PortfolioMetricsResponse;
import com.smelend.smelendbackend.service.application.ApplicationService;
import com.smelend.smelendbackend.service.collections.CollectionsService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import com.smelend.smelendbackend.service.kyc.KycService;
import com.smelend.smelendbackend.service.onboarding.PromoterService;
import com.smelend.smelendbackend.service.onboarding.SmeService;
import com.smelend.smelendbackend.service.operations.OfferService;
import com.smelend.smelendbackend.service.risk.RiskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/monitor")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMonitorController {

    private final SmeService smeService;
    private final PromoterService promoterService;
    private final KycService kycService;
    private final ApplicationService applicationService;
    private final OfferService offerService;
    private final CollectionsService collectionsService;
    private final RiskService riskService;
    private final AuditLogService auditLogService;

    public AdminMonitorController(SmeService smeService,
                                  PromoterService promoterService,
                                  KycService kycService,
                                  ApplicationService applicationService,
                                  OfferService offerService,
                                  CollectionsService collectionsService,
                                  RiskService riskService,
                                  AuditLogService auditLogService) {
        this.smeService = smeService;
        this.promoterService = promoterService;
        this.kycService = kycService;
        this.applicationService = applicationService;
        this.offerService = offerService;
        this.collectionsService = collectionsService;
        this.riskService = riskService;
        this.auditLogService = auditLogService;
    }


    @GetMapping("/smes")
    public ApiResponse<List<SmeResponse>> allSmes() {
        return ApiResponse.ok("SMEs fetched", smeService.listMine());
    }

    @GetMapping("/smes/{smeId}")
    public ApiResponse<SmeResponse> getSme(@PathVariable Long smeId) {
        return ApiResponse.ok("SME fetched", smeService.get(smeId));
    }

    @GetMapping("/smes/{smeId}/promoters")
    public ApiResponse<List<PromoterResponse>> promotersBySme(@PathVariable Long smeId) {
        return ApiResponse.ok("Promoters fetched", promoterService.listBySme(smeId));
    }

    @GetMapping("/promoters/{promoterId}")
    public ApiResponse<PromoterResponse> getPromoter(@PathVariable Long promoterId) {
        return ApiResponse.ok("Promoter fetched", promoterService.get(promoterId));
    }


    @GetMapping("/kyc")
    public ApiResponse<List<KycResponse>> allKyc() {
        return ApiResponse.ok("All KYC fetched", kycService.listAll());
    }

    @GetMapping("/kyc/pending")
    public ApiResponse<List<KycResponse>> pendingKyc() {
        return ApiResponse.ok("Pending KYC fetched", kycService.listAllPending());
    }

    @GetMapping("/kyc/smes/{smeId}")
    public ApiResponse<List<KycResponse>> kycBySme(@PathVariable Long smeId) {
        return ApiResponse.ok("KYC fetched", kycService.listBySme(smeId));
    }


    @GetMapping("/applications")
    public ApiResponse<List<ApplicationResponse>> allApplications() {
        return ApiResponse.ok("Applications fetched", applicationService.listAll());
    }

    @GetMapping("/applications/{appId}")
    public ApiResponse<ApplicationResponse> getApplication(@PathVariable Long appId) {
        return ApiResponse.ok("Application fetched", applicationService.get(appId));
    }


    @GetMapping("/offers")
    public ApiResponse<List<OfferResponse>> allOffers() {
        return ApiResponse.ok("Offers fetched", offerService.listMineOrAll());
    }


    @GetMapping("/delinquencies")
    public ApiResponse<List<DelinquencyResponse>> allDelinquencies() {
        return ApiResponse.ok("Delinquencies fetched", collectionsService.listAllDelinquencies());
    }

    @GetMapping("/loan-accounts/{loanAccountId}/ptp")
    public ApiResponse<List<PtpResponse>> ptpByAccount(@PathVariable Long loanAccountId) {
        return ApiResponse.ok("PTPs fetched", collectionsService.listPtps(loanAccountId));
    }


    @GetMapping("/risk/metrics")
    public ApiResponse<PortfolioMetricsResponse> riskMetrics() {
        return ApiResponse.ok("Portfolio metrics fetched", riskService.portfolioMetrics());
    }


    @GetMapping("/audit-logs")
    public ApiResponse<List<AuditLogResponse>> auditLogs() {
        return ApiResponse.ok("Audit logs fetched", auditLogService.listAll());
    }

    @GetMapping("/audit-logs/actor/{userId}")
    public ApiResponse<List<AuditLogResponse>> auditByActor(@PathVariable Long userId) {
        return ApiResponse.ok("Audit logs fetched", auditLogService.listByActor(userId));
    }
}
