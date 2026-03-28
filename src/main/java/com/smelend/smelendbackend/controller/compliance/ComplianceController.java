package com.smelend.smelendbackend.controller.compliance;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.compliance.AuditLogResponse;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compliance/audit-logs")
@PreAuthorize("hasAnyRole('COMPLIANCE')")
public class ComplianceController {

    private final AuditLogService auditLogService;

    public ComplianceController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<AuditLogResponse>> listAll() {
        return ApiResponse.ok("Audit logs fetched", auditLogService.listAll());
    }

    @GetMapping("/ref")
    public ApiResponse<List<AuditLogResponse>> listByRef(
            @RequestParam String refType,
            @RequestParam Long refId
    ) {
        return ApiResponse.ok("Audit logs fetched", auditLogService.listByRef(refType, refId));
    }

    @GetMapping("/actor/{actorUserId}")
    public ApiResponse<List<AuditLogResponse>> listByActor(@PathVariable Long actorUserId) {
        return ApiResponse.ok("Audit logs fetched", auditLogService.listByActor(actorUserId));
    }
}