package com.smelend.smelendbackend.controller.servicing;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.LoanAccountResponse;
import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
