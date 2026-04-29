package com.smelend.smelendbackend.controller.kyc;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.kyc.InitKycRequest;
import com.smelend.smelendbackend.dto.kyc.KycActionRequest;
import com.smelend.smelendbackend.dto.kyc.KycResponse;
import com.smelend.smelendbackend.service.kyc.KycService;
import com.smelend.smelendbackend.service.kyc.KycVerificationService;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.entity.LoanApplication;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kyc")
public class KycController {

    private final KycService              kycService;
    private final KycVerificationService  kycVerificationService;
    private final LoanApplicationRepository appRepo;

    public KycController(KycService kycService,
                      KycVerificationService kycVerificationService,
                      LoanApplicationRepository appRepo) {
        this.kycService             = kycService;
        this.kycVerificationService = kycVerificationService;
        this.appRepo               = appRepo;
    }

    @PostMapping("/initialize")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
    public ApiResponse<KycResponse> initialize(@Valid @RequestBody InitKycRequest req) {
        return ApiResponse.ok("KYC initialized", kycService.initializeKYC(req));
    }

    @PatchMapping("/{kycId}/verify")
    @PreAuthorize("hasAnyRole('AGENT')")
    public ApiResponse<KycResponse> verify(@PathVariable Long kycId,
                                           @RequestBody(required = false) KycActionRequest req) {
        return ApiResponse.ok("KYC verified", kycService.verify(kycId, req));
    }


    @PatchMapping("/{kycId}/reject")
    @PreAuthorize("hasAnyRole('AGENT')")
    public ApiResponse<KycResponse> reject(@PathVariable Long kycId,
                                           @RequestBody(required = false) KycActionRequest req) {
        return ApiResponse.ok("KYC rejected", kycService.reject(kycId, req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ApiResponse<List<KycResponse>> listAll() {
        return ApiResponse.ok("All KYC records fetched", kycService.listAll());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ApiResponse<List<KycResponse>> listPending() {
        return ApiResponse.ok("Pending KYC queue fetched", kycService.listAllPending());
    }

    @GetMapping("/{kycId}")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ApiResponse<KycResponse> getById(@PathVariable Long kycId) {
        return ApiResponse.ok("KYC fetched", kycService.getById(kycId));
    }

    @GetMapping("/application/{appId}")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ApiResponse<KycResponse> getByApplication(@PathVariable Long appId) {
        return ApiResponse.ok("KYC fetched", kycService.getByApplicationId(appId));
    }

    @GetMapping("/smes/{smeId}")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ApiResponse<List<KycResponse>> listBySme(@PathVariable Long smeId) {
        return ApiResponse.ok("KYC list fetched", kycService.listBySme(smeId));
    }

    @GetMapping("/applications/{appId}/readiness")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ApiResponse<Map<String, Object>> checkReadiness(@PathVariable Long appId) {
        LoanApplication app = appRepo.findById(appId)
                .orElseThrow(() -> new com.smelend.smelendbackend.exception.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Application #" + appId + " not found"));
        KycVerificationService.KycReadinessResult result = kycVerificationService.checkAll(app);
        return ApiResponse.ok("KYC readiness checked", Map.of(
                "ready",    result.ready(),
                "failures", result.failures(),
                "summary",  result.ready()
                        ? "All KYC checks passed."
                        : result.failures().size() + " check(s) blocking submission."
        ));
    }
}
