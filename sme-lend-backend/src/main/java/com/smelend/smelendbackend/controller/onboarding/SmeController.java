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
public class SmeController {

    private final SmeService smeService;

    public SmeController(SmeService smeService) {
        this.smeService = smeService;
    }

    // ── WRITE: APPLICANT / AGENT only ─────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
    public ApiResponse<SmeResponse> create(@Valid @RequestBody CreateSmeRequest req) {
        return ApiResponse.ok("SME created", smeService.create(req));
    }

    @PutMapping("/{smeId}")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
    public ApiResponse<SmeResponse> update(@PathVariable Long smeId,
                                           @Valid @RequestBody UpdateSmeRequest req) {
        return ApiResponse.ok("SME updated", smeService.update(smeId, req));
    }

    // ── READ: Admin/UW can monitor, Applicant/Agent see own ───────────

    @GetMapping("/{smeId}")
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ApiResponse<SmeResponse> get(@PathVariable Long smeId) {
        return ApiResponse.ok("SME fetched", smeService.get(smeId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('APPLICANT','AGENT','ADMIN','UNDERWRITER')")
    public ApiResponse<List<SmeResponse>> list() {
        return ApiResponse.ok("SMEs fetched", smeService.listMine());
    }
}
