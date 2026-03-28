package com.smelend.smelendbackend.controller.risk;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.risk.PortfolioMetricsResponse;
import com.smelend.smelendbackend.service.risk.RiskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/risk")
@PreAuthorize("hasAnyRole('RISK')")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/portfolio/metrics")
    public ApiResponse<PortfolioMetricsResponse> metrics() {
        return ApiResponse.ok("Portfolio metrics fetched", riskService.portfolioMetrics());
    }
}