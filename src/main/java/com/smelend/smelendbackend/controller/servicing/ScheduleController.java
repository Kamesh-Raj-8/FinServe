package com.smelend.smelendbackend.controller.servicing;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.servicing.schedule.ScheduleResponse;
import com.smelend.smelendbackend.service.servicing.EmiScheduleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/servicing/loan-accounts/{loanAccountId}/schedule")
@PreAuthorize("hasAnyRole('OPERATIONS','SERVICING','APPLICANT','AGENT')")
public class ScheduleController {

    private final EmiScheduleService scheduleService;

    public ScheduleController(EmiScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ApiResponse<List<ScheduleResponse>> list(@PathVariable Long loanAccountId) {
        return ApiResponse.ok("Schedule fetched", scheduleService.list(loanAccountId));
    }
}