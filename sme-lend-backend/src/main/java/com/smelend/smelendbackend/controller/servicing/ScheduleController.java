package com.smelend.smelendbackend.controller.servicing;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.servicing.schedule.ScheduleResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.LoanAccountResponse;
import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import com.smelend.smelendbackend.exception.ApiException;
import org.springframework.http.HttpStatus;
import com.smelend.smelendbackend.service.servicing.EmiScheduleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/servicing/loan-accounts/{loanAccountId}/schedule")
@PreAuthorize("hasAnyRole('SERVICING','APPLICANT','AGENT','ADMIN','OPERATIONS')")
public class ScheduleController {

    private final EmiScheduleService    scheduleService;
    private final LoanAccountRepository loanRepo;

    public ScheduleController(EmiScheduleService scheduleService,
                           LoanAccountRepository loanRepo) {
        this.scheduleService = scheduleService;
        this.loanRepo        = loanRepo;
    }

    @GetMapping
    public ApiResponse<List<ScheduleResponse>> list(@PathVariable Long loanAccountId) {
        return ApiResponse.ok("Schedule fetched", scheduleService.list(loanAccountId));
    }

}

/**
 * Standalone lookup endpoint — separate controller to avoid URL collision
 * with ScheduleController which is mapped to /servicing/loan-accounts/{id}/schedule.
 */
@RestController
@RequestMapping("/servicing/loan-accounts")
class LoanAccountByApplicationController {

    private final LoanAccountRepository loanRepo;

    LoanAccountByApplicationController(LoanAccountRepository loanRepo) {
        this.loanRepo = loanRepo;
    }

    /** GET /servicing/loan-accounts/by-application/{appId} */
    @GetMapping("/by-application/{appId}")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','SERVICING','OPERATIONS','UNDERWRITER')")
    public ApiResponse<LoanAccountResponse> getByApplication(@PathVariable Long appId) {
        LoanAccount loan = loanRepo.findByApplication_ApplicationId(appId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No loan account for application #" + appId + ". Not yet disbursed."));
        return ApiResponse.ok("Loan account found", LoanAccountResponse.builder()
                .loanAccountId(loan.getLoanAccountId())
                .applicationId(loan.getApplication().getApplicationId())
                .accountNumber(loan.getAccountNumber())
                .principalSanctioned(loan.getPrincipalSanctioned())
                .interestRate(loan.getInterestRate())
                .tenorMonths(loan.getTenorMonths())
                .startDate(loan.getStartDate())
                .status(loan.getStatus())
                .build());
    }
}
