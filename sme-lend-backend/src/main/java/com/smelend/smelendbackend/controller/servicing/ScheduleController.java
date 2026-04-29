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
