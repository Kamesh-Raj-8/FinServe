package com.smelend.smelendbackend.controller.operations;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.DisburseRequest;
import com.smelend.smelendbackend.dto.operations.disbursement.DisbursementResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.LoanAccountResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.PendingDisbursementDto;
import com.smelend.smelendbackend.dto.application.ApplicationResponse;
import com.smelend.smelendbackend.service.application.ApplicationService;
import com.smelend.smelendbackend.service.operations.DisbursementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ops")
public class DisbursementController {

    private final DisbursementService disbursementService;
    private final ApplicationService  applicationService;

    public DisbursementController(DisbursementService disbursementService,
                               ApplicationService applicationService) {
        this.disbursementService = disbursementService;
        this.applicationService  = applicationService;
    }



    @PostMapping("/applications/{applicationId}/disburse")
    @PreAuthorize("hasAnyRole('OPERATIONS')")
    public ApiResponse<DisbursementResponse> disburse(
            @PathVariable Long applicationId,
            @Valid @RequestBody DisburseRequest req) {
        return ApiResponse.ok("Disbursed + loan account created + schedule generated",
                disbursementService.disburse(applicationId, req));
    }


    @GetMapping("/pending-disbursements")
    @PreAuthorize("hasAnyRole('OPERATIONS','ADMIN')")
    public ApiResponse<List<PendingDisbursementDto>> listPendingDisbursements() {
        return ApiResponse.ok("Pending disbursements fetched",
                disbursementService.listPendingDisbursements());
    }

    @GetMapping("/loan-accounts/{loanAccountId}")
    @PreAuthorize("hasAnyRole('OPERATIONS','ADMIN','SERVICING')")
    public ApiResponse<LoanAccountResponse> getLoanAccount(@PathVariable Long loanAccountId) {
        return ApiResponse.ok("Loan account fetched",
                disbursementService.getLoanAccount(loanAccountId));
    }

    @GetMapping("/applications/approved")
    @PreAuthorize("hasAnyRole('OPERATIONS','ADMIN')")
    public ApiResponse<java.util.List<ApplicationResponse>> listApproved() {
        return ApiResponse.ok("Approved apps fetched", applicationService.listApprovedForOperations());
    }

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasAnyRole('OPERATIONS','ADMIN')")
    public ApiResponse<ApplicationResponse> getApplication(@PathVariable Long applicationId) {
        return ApiResponse.ok("Application fetched", applicationService.getForOperations(applicationId));
    }
}
