package com.smelend.smelendbackend.controller.servicing;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.servicing.repayment.PostRepaymentRequest;
import com.smelend.smelendbackend.dto.servicing.repayment.RepaymentResponse;
import com.smelend.smelendbackend.service.servicing.RepaymentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/servicing/repayments")
public class RepaymentController {

    private final RepaymentService repaymentService;

    public RepaymentController(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    // ── WRITE: SERVICING/ADMIN only — granular check enforced in RepaymentService ──

    @PostMapping
    @PreAuthorize("hasAnyRole('SERVICING','ADMIN')")
    public ApiResponse<RepaymentResponse> post(@Valid @RequestBody PostRepaymentRequest req) {
        return ApiResponse.ok("Repayment posted", repaymentService.post(req));
    }

    // ── READ: Servicing, Applicant, Agent, Admin (monitoring) ─────────

    @GetMapping("/loan-accounts/{loanAccountId}")
    @PreAuthorize("hasAnyRole('SERVICING','APPLICANT','AGENT','ADMIN')")
    public ApiResponse<List<RepaymentResponse>> list(@PathVariable Long loanAccountId) {
        return ApiResponse.ok("Repayments fetched", repaymentService.list(loanAccountId));
    }
}
