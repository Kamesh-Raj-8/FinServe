package com.smelend.smelendbackend.controller.admin;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.eligibility.*;
import com.smelend.smelendbackend.dto.fee.*;
import com.smelend.smelendbackend.service.eligibility.EligibilityService;
import com.smelend.smelendbackend.service.fee.FeeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class EligibilityFeeController {

    private final EligibilityService eligibilityService;
    private final FeeService         feeService;

    public EligibilityFeeController(EligibilityService eligibilityService, FeeService feeService) {
        this.eligibilityService = eligibilityService;
        this.feeService         = feeService;
    }

    @GetMapping("/eligibility-policies")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<EligibilityPolicyResponse>> listPolicies() {
        return ApiResponse.ok("Policies fetched", eligibilityService.listAll());
    }

    @GetMapping("/eligibility-policies/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNDERWRITER','OPERATIONS')")
    public ApiResponse<List<EligibilityPolicyResponse>> listByProduct(@PathVariable Long productId) {
        return ApiResponse.ok("Policies fetched", eligibilityService.listByProduct(productId));
    }

    @PostMapping("/eligibility-policies")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EligibilityPolicyResponse> createPolicy(@Valid @RequestBody EligibilityPolicyRequest req) {
        return ApiResponse.ok("Policy created", eligibilityService.create(req));
    }

    @DeleteMapping("/eligibility-policies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deactivatePolicy(@PathVariable Long id) {
        eligibilityService.deactivate(id);
        return ApiResponse.ok("Policy deactivated", null);
    }

    @GetMapping("/eligibility-check/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNDERWRITER','OPERATIONS')")
    public ApiResponse<EligibilityCheckResult> checkEligibility(@PathVariable Long applicationId) {
        return ApiResponse.ok("Eligibility checked", eligibilityService.checkApplication(applicationId));
    }

    @GetMapping("/fee-configs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<FeeConfigResponse>> listFees() {
        return ApiResponse.ok("Fee configs fetched", feeService.listAll());
    }

    @GetMapping("/fee-configs/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public ApiResponse<List<FeeConfigResponse>> listFeesByProduct(@PathVariable Long productId) {
        return ApiResponse.ok("Fee configs fetched", feeService.listByProduct(productId));
    }

    @PostMapping("/fee-configs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeeConfigResponse> createFee(@Valid @RequestBody FeeConfigRequest req) {
        return ApiResponse.ok("Fee config created", feeService.create(req));
    }

    @DeleteMapping("/fee-configs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deactivateFee(@PathVariable Long id) {
        feeService.deactivate(id);
        return ApiResponse.ok("Fee config deactivated", null);
    }
}
