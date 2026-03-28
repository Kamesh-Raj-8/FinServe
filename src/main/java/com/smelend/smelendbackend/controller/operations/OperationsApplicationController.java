package com.smelend.smelendbackend.controller.operations;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.application.ApplicationResponse;
import com.smelend.smelendbackend.service.application.ApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ops/applications")
public class OperationsApplicationController {

    private final ApplicationService applicationService;

    public OperationsApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('OPERATIONS')")
    public ApiResponse<ApplicationResponse> getApplication(@PathVariable Long applicationId) {
        return ApiResponse.ok(
                "Application fetched",
                applicationService.getForOperations(applicationId)
        );
    }

    @GetMapping("/approved")
    @PreAuthorize("hasAnyRole('OPERATIONS')")
    public ApiResponse<List<ApplicationResponse>> listApproved() {
        return ApiResponse.ok(
                "Approved applications fetched",
                applicationService.listApprovedForOperations()
        );
    }
}