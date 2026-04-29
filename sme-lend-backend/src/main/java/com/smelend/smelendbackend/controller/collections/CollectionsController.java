package com.smelend.smelendbackend.controller.collections;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.collections.CreatePtpRequest;
import com.smelend.smelendbackend.dto.collections.DelinquencyResponse;
import com.smelend.smelendbackend.dto.collections.PtpResponse;
import com.smelend.smelendbackend.entity.enums.PtpStatus;
import com.smelend.smelendbackend.dto.charge.ChargeRequest;
import com.smelend.smelendbackend.dto.charge.ChargeResponse;
import com.smelend.smelendbackend.service.charge.ChargeService;
import com.smelend.smelendbackend.service.collections.CollectionsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/collections")
public class CollectionsController {

    private final CollectionsService collectionsService;
    private final ChargeService       chargeService;

    public CollectionsController(CollectionsService collectionsService,
                               ChargeService chargeService) {
        this.collectionsService = collectionsService;
        this.chargeService      = chargeService;
    }

    @GetMapping("/loan-accounts/{loanAccountId}/delinquency")
    @PreAuthorize("hasAnyRole('COLLECTIONS','ADMIN','SERVICING')")
    public ApiResponse<DelinquencyResponse> getDelinquency(@PathVariable Long loanAccountId) {
        return ApiResponse.ok("Delinquency fetched", collectionsService.getDelinquency(loanAccountId));
    }

    @GetMapping("/delinquencies")
    @PreAuthorize("hasAnyRole('COLLECTIONS','ADMIN','RISK')")
    public ApiResponse<List<DelinquencyResponse>> listAll() {
        return ApiResponse.ok("Delinquencies fetched", collectionsService.listAllDelinquencies());
    }

    @PostMapping("/ptp")
    @PreAuthorize("hasAnyRole('COLLECTIONS')")
    public ApiResponse<PtpResponse> createPtp(@Valid @RequestBody CreatePtpRequest req) {
        return ApiResponse.ok("PTP created", collectionsService.createPtp(req));
    }

    @GetMapping("/loan-accounts/{loanAccountId}/ptp")
    @PreAuthorize("hasAnyRole('COLLECTIONS')")
    public ApiResponse<List<PtpResponse>> listPtps(@PathVariable Long loanAccountId) {
        return ApiResponse.ok("PTPs fetched", collectionsService.listPtps(loanAccountId));
    }

    @PatchMapping("/ptp/{ptpId}/status")
    @PreAuthorize("hasAnyRole('COLLECTIONS')")
    public ApiResponse<PtpResponse> updatePtpStatus(@PathVariable Long ptpId, @RequestParam PtpStatus status) {
        return ApiResponse.ok("PTP status updated", collectionsService.updatePtpStatus(ptpId, status));
    }


    @PostMapping("/charges")
    @PreAuthorize("hasAnyRole('OPERATIONS','COLLECTIONS')")
    public ApiResponse<ChargeResponse> postCharge(@Valid @RequestBody ChargeRequest req) {
        return ApiResponse.ok("Charge posted", chargeService.post(req));
    }

    @PatchMapping("/charges/{chargeId}/waive")
    @PreAuthorize("hasAnyRole('OPERATIONS','COLLECTIONS')")
    public ApiResponse<ChargeResponse> waiveCharge(@PathVariable Long chargeId) {
        return ApiResponse.ok("Charge waived", chargeService.waive(chargeId));
    }

    @GetMapping("/charges/loan/{loanAccountId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','COLLECTIONS','SERVICING','APPLICANT')")
    public ApiResponse<java.util.List<ChargeResponse>> listCharges(@PathVariable Long loanAccountId) {
        return ApiResponse.ok("Charges fetched", chargeService.listByLoanAccount(loanAccountId));
    }
}
