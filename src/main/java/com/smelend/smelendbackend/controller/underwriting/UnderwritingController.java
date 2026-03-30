package com.smelend.smelendbackend.controller.underwriting;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.application.ApplicationResponse;
import com.smelend.smelendbackend.dto.underwriting.UwDecisionRequest;
import com.smelend.smelendbackend.dto.underwriting.UwReviewResponse;
import com.smelend.smelendbackend.service.underwriting.UnderwritingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/uw")
@PreAuthorize("hasAnyRole('UNDERWRITER','ADMIN')")
public class UnderwritingController {

    private final UnderwritingService underwritingService;

    public UnderwritingController(UnderwritingService underwritingService) {
        this.underwritingService = underwritingService;
    }

    @GetMapping("/queue")
    public ApiResponse<List<ApplicationResponse>> queue() {
        return ApiResponse.ok("Underwriting queue fetched", underwritingService.queue());
    }

    @PostMapping("/applications/{applicationId}/decision")
    public ApiResponse<UwReviewResponse> decide(
            @PathVariable Long applicationId,
            @Valid @RequestBody UwDecisionRequest req
    ) {
        return ApiResponse.ok("Decision recorded", underwritingService.decide(applicationId, req));
    }
}