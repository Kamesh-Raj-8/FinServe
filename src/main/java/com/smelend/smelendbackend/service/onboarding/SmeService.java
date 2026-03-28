package com.smelend.smelendbackend.service.onboarding;

import com.smelend.smelendbackend.dto.onboarding.sme.CreateSmeRequest;
import com.smelend.smelendbackend.dto.onboarding.sme.SmeResponse;
import com.smelend.smelendbackend.dto.onboarding.sme.UpdateSmeRequest;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.Sme;
import com.smelend.smelendbackend.entity.enums.AuditAction;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.SmeRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmeService {

    private final SmeRepository smeRepo;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public SmeService(SmeRepository smeRepo, CurrentUserService currentUserService, AuditLogService auditLogService) {
        this.smeRepo = smeRepo;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public SmeResponse create(CreateSmeRequest req) {
        AppUser me = currentUserService.getCurrentUser();

        Sme saved = smeRepo.save(Sme.builder()
                .legalName(req.getLegalName())
                .tradeName(req.getTradeName())
                .businessType(req.getBusinessType())
                .industry(req.getIndustry())
                .address(req.getAddress())
                .gstNo(req.getGstNo())
                .status(StatusFlag.ACTIVE)
                .createdBy(me)
                .build());

        auditLogService.log(me, AuditAction.SME_CREATED, "SME", saved.getSmeId(), "SME created: " + saved.getLegalName());

        return toDto(saved);
    }

    public SmeResponse update(Long smeId, UpdateSmeRequest req) {
        AppUser me = currentUserService.getCurrentUser();

        Sme sme = smeRepo.findById(smeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SME not found"));

        // Ownership check: only creator or ADMIN can edit
        boolean owner = sme.getCreatedBy() != null && sme.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner && !currentUserService.isAdmin(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not allowed to edit this SME");
        }

        sme.setLegalName(req.getLegalName());
        sme.setTradeName(req.getTradeName());
        sme.setBusinessType(req.getBusinessType());
        sme.setIndustry(req.getIndustry());
        sme.setAddress(req.getAddress());
        sme.setGstNo(req.getGstNo());

        sme = smeRepo.save(sme);

        auditLogService.log(me, AuditAction.SME_UPDATED, "SME", sme.getSmeId(), "SME updated: " + sme.getLegalName());

        return toDto(sme);
    }

    public SmeResponse get(Long smeId) {
        AppUser me = currentUserService.getCurrentUser();

        Sme sme = smeRepo.findById(smeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SME not found"));

        boolean owner = sme.getCreatedBy() != null && sme.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner && !currentUserService.isAdmin(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not allowed to view this SME");
        }

        return toDto(sme);
    }

    public List<SmeResponse> listMine() {
        AppUser me = currentUserService.getCurrentUser();
        return smeRepo.findByCreatedBy_UserId(me.getUserId()).stream().map(this::toDto).toList();
    }

    private SmeResponse toDto(Sme s) {
        return SmeResponse.builder()
                .smeId(s.getSmeId())
                .legalName(s.getLegalName())
                .tradeName(s.getTradeName())
                .businessType(s.getBusinessType())
                .industry(s.getIndustry())
                .address(s.getAddress())
                .gstNo(s.getGstNo())
                .status(s.getStatus())
                .createdByUserId(s.getCreatedBy() != null ? s.getCreatedBy().getUserId() : null)
                .createdByEmail(s.getCreatedBy() != null ? s.getCreatedBy().getEmail() : null)
                .build();
    }
}