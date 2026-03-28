package com.smelend.smelendbackend.controller.operations;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.DisburseRequest;
import com.smelend.smelendbackend.dto.operations.disbursement.DisbursementResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.LoanAccountResponse;
import com.smelend.smelendbackend.service.operations.DisbursementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ops")
@PreAuthorize("hasAnyRole('OPERATIONS')")
public class DisbursementController {

    private final DisbursementService disbursementService;

    public DisbursementController(DisbursementService disbursementService) {
        this.disbursementService = disbursementService;
    }

    @PostMapping("/applications/{applicationId}/disburse")
    public ApiResponse<DisbursementResponse> disburse(
            @PathVariable Long applicationId,
            @Valid @RequestBody DisburseRequest req
    ) {
        return ApiResponse.ok("Disbursed + loan account created + schedule generated",
                disbursementService.disburse(applicationId, req));
    }

    @GetMapping("/loan-accounts/{loanAccountId}")
    public ApiResponse<LoanAccountResponse> getLoanAccount(@PathVariable Long loanAccountId) {
        return ApiResponse.ok("Loan account fetched", disbursementService.getLoanAccount(loanAccountId));
    }
}