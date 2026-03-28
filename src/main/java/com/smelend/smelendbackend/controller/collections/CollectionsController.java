package com.smelend.smelendbackend.controller.collections;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.collections.CreatePtpRequest;
import com.smelend.smelendbackend.dto.collections.DelinquencyResponse;
import com.smelend.smelendbackend.dto.collections.PtpResponse;
import com.smelend.smelendbackend.entity.enums.PtpStatus;
import com.smelend.smelendbackend.service.collections.CollectionsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/collections")
public class CollectionsController {

    private final CollectionsService collectionsService;

    public CollectionsController(CollectionsService collectionsService) {
        this.collectionsService = collectionsService;
    }

    // View delinquency for a loan account
    @GetMapping("/loan-accounts/{loanAccountId}/delinquency")
    @PreAuthorize("hasAnyRole('COLLECTIONS')")
    public ApiResponse<DelinquencyResponse> getDelinquency(@PathVariable Long loanAccountId) {
        return ApiResponse.ok("Delinquency fetched", collectionsService.getDelinquency(loanAccountId));
    }

    // Collections list all delinquencies
    @GetMapping("/delinquencies")
    @PreAuthorize("hasAnyRole('COLLECTIONS')")
    public ApiResponse<List<DelinquencyResponse>> listAll() {
        return ApiResponse.ok("Delinquencies fetched", collectionsService.listAllDelinquencies());
    }

    // Collections creates PTP
    @PostMapping("/ptp")
    @PreAuthorize("hasAnyRole('COLLECTIONS')")
    public ApiResponse<PtpResponse> createPtp(@Valid @RequestBody CreatePtpRequest req) {
        return ApiResponse.ok("PTP created", collectionsService.createPtp(req));
    }

    // List PTPs for a loan account
    @GetMapping("/loan-accounts/{loanAccountId}/ptp")
    @PreAuthorize("hasAnyRole('COLLECTIONS')")
    public ApiResponse<List<PtpResponse>> listPtps(@PathVariable Long loanAccountId) {
        return ApiResponse.ok("PTPs fetched", collectionsService.listPtps(loanAccountId));
    }

    // Update PTP status
    @PatchMapping("/ptp/{ptpId}/status")
    @PreAuthorize("hasAnyRole('COLLECTIONS')")
    public ApiResponse<PtpResponse> updatePtpStatus(@PathVariable Long ptpId, @RequestParam PtpStatus status) {
        return ApiResponse.ok("PTP status updated", collectionsService.updatePtpStatus(ptpId, status));
    }
}