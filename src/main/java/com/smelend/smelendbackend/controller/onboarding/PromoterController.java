package com.smelend.smelendbackend.controller.onboarding;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.onboarding.promoter.AddPromoterRequest;
import com.smelend.smelendbackend.dto.onboarding.promoter.PromoterResponse;
import com.smelend.smelendbackend.service.onboarding.PromoterService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/onboarding")
@PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
public class PromoterController {

    private final PromoterService promoterService;

    public PromoterController(PromoterService promoterService) {
        this.promoterService = promoterService;
    }

    @PostMapping("/smes/{smeId}/promoters")
    public ApiResponse<PromoterResponse> addPromoter(
            @PathVariable Long smeId,
            @Valid @RequestBody AddPromoterRequest req
    ) {
        return ApiResponse.ok("Promoter added", promoterService.addPromoter(smeId, req));
    }

    @GetMapping("/smes/{smeId}/promoters")
    public ApiResponse<List<PromoterResponse>> listPromoters(@PathVariable Long smeId) {
        return ApiResponse.ok("Promoters fetched", promoterService.listBySme(smeId));
    }

    @GetMapping("/promoters/{promoterId}")
    public ApiResponse<PromoterResponse> getPromoter(@PathVariable Long promoterId) {
        return ApiResponse.ok("Promoter fetched", promoterService.get(promoterId));
    }
}