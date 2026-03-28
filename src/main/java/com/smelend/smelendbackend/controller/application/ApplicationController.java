package com.smelend.smelendbackend.controller.application;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.application.ApplicationResponse;
import com.smelend.smelendbackend.dto.application.CreateApplicationRequest;
import com.smelend.smelendbackend.service.application.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
@PreAuthorize("hasAnyRole('APPLICANT','AGENT')")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ApiResponse<ApplicationResponse> create(@Valid @RequestBody CreateApplicationRequest req) {
        return ApiResponse.ok("Application created", applicationService.create(req));
    }

    @GetMapping
    public ApiResponse<List<ApplicationResponse>> listMine() {
        return ApiResponse.ok("Applications fetched", applicationService.listMine());
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<ApplicationResponse> get(@PathVariable Long applicationId) {
        return ApiResponse.ok("Application fetched", applicationService.get(applicationId));
    }

    @PatchMapping("/{applicationId}/submit")
    public ApiResponse<ApplicationResponse> submit(@PathVariable Long applicationId) {
        return ApiResponse.ok("Application submitted and routed to underwriter", applicationService.submit(applicationId));
    }
}