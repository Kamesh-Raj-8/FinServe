package com.smelend.smelendbackend.controller.kyc;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.kyc.CreateKycRequest;
import com.smelend.smelendbackend.dto.kyc.KycActionRequest;
import com.smelend.smelendbackend.dto.kyc.KycResponse;
import com.smelend.smelendbackend.service.kyc.KycService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    // Applicant/Agent creates a KYC record
    @PostMapping
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
    public ApiResponse<KycResponse> create(@Valid @RequestBody CreateKycRequest req) {
        return ApiResponse.ok("KYC created", kycService.create(req));
    }

    // Applicant/Agent views SME KYC list
    @GetMapping("/smes/{smeId}")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
    public ApiResponse<List<KycResponse>> listBySme(@PathVariable Long smeId) {
        return ApiResponse.ok("KYC list fetched", kycService.listBySme(smeId));
    }

    // Agent verifies KYC
    @PatchMapping("/{kycId}/verify")
    @PreAuthorize("hasAnyRole('AGENT')")
    public ApiResponse<KycResponse> verify(@PathVariable Long kycId, @RequestBody(required = false) KycActionRequest req) {
        return ApiResponse.ok("KYC verified", kycService.verify(kycId, req));
    }

    // Agent rejects KYC
    @PatchMapping("/{kycId}/reject")
    @PreAuthorize("hasAnyRole('AGENT')")
    public ApiResponse<KycResponse> reject(@PathVariable Long kycId, @RequestBody(required = false) KycActionRequest req) {
        return ApiResponse.ok("KYC rejected", kycService.reject(kycId, req));
    }
}