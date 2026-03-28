package com.smelend.smelendbackend.controller.onboarding;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.onboarding.sme.CreateSmeRequest;
import com.smelend.smelendbackend.dto.onboarding.sme.SmeResponse;
import com.smelend.smelendbackend.dto.onboarding.sme.UpdateSmeRequest;
import com.smelend.smelendbackend.service.onboarding.SmeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/onboarding/smes")
@PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
public class SmeController {

    private final SmeService smeService;

    public SmeController(SmeService smeService) {
        this.smeService = smeService;
    }

    @PostMapping
    public ApiResponse<SmeResponse> create(@Valid @RequestBody CreateSmeRequest req) {
        return ApiResponse.ok("SME created", smeService.create(req));
    }

    @PutMapping("/{smeId}")
    public ApiResponse<SmeResponse> update(@PathVariable Long smeId, @Valid @RequestBody UpdateSmeRequest req) {
        return ApiResponse.ok("SME updated", smeService.update(smeId, req));
    }

    @GetMapping("/{smeId}")
    public ApiResponse<SmeResponse> get(@PathVariable Long smeId) {
        return ApiResponse.ok("SME fetched", smeService.get(smeId));
    }

    @GetMapping
    public ApiResponse<List<SmeResponse>> listMine() {
        return ApiResponse.ok("My SMEs fetched", smeService.listMine());
    }
}