package com.smelend.smelendbackend.service.onboarding;

import com.smelend.smelendbackend.dto.onboarding.promoter.AddPromoterRequest;
import com.smelend.smelendbackend.dto.onboarding.promoter.PromoterResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.Promoter;
import com.smelend.smelendbackend.entity.Sme;
import com.smelend.smelendbackend.entity.enums.KycStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.PromoterRepository;
import com.smelend.smelendbackend.repository.SmeRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromoterService {

    private final PromoterRepository promoterRepo;
    private final SmeRepository smeRepo;
    private final CurrentUserService currentUserService;

    public PromoterService(PromoterRepository promoterRepo, SmeRepository smeRepo, CurrentUserService currentUserService) {
        this.promoterRepo = promoterRepo;
        this.smeRepo = smeRepo;
        this.currentUserService = currentUserService;
    }

    public PromoterResponse addPromoter(Long smeId, AddPromoterRequest req) {
        AppUser me = currentUserService.getCurrentUser();

        Sme sme = smeRepo.findById(smeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SME not found"));

        boolean owner = sme.getCreatedBy() != null && sme.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner && !currentUserService.isAdmin(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not allowed to add promoter for this SME");
        }

        Promoter saved = promoterRepo.save(Promoter.builder()
                .sme(sme)
                .promoterName(req.getPromoterName())
                .mobile(req.getMobile())
                .ownershipPct(req.getOwnershipPct())
                .kycStatus(KycStatus.PENDING)
                .createdBy(me)
                .build());

        return toDto(saved);
    }

    public List<PromoterResponse> listBySme(Long smeId) {
        AppUser me = currentUserService.getCurrentUser();

        Sme sme = smeRepo.findById(smeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SME not found"));

        boolean owner = sme.getCreatedBy() != null && sme.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner && !currentUserService.isAdmin(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not allowed to view promoters for this SME");
        }

        return promoterRepo.findBySme_SmeId(smeId).stream().map(this::toDto).toList();
    }

    public PromoterResponse get(Long promoterId) {
        AppUser me = currentUserService.getCurrentUser();

        Promoter p = promoterRepo.findById(promoterId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Promoter not found"));

        Sme sme = p.getSme();
        boolean owner = sme != null && sme.getCreatedBy() != null && sme.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner && !currentUserService.isAdmin(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not allowed to view this promoter");
        }

        return toDto(p);
    }

    private PromoterResponse toDto(Promoter p) {
        return PromoterResponse.builder()
                .promoterId(p.getPromoterId())
                .smeId(p.getSme() != null ? p.getSme().getSmeId() : null)
                .promoterName(p.getPromoterName())
                .mobile(p.getMobile())
                .ownershipPct(p.getOwnershipPct())
                .kycStatus(p.getKycStatus())
                .createdByUserId(p.getCreatedBy() != null ? p.getCreatedBy().getUserId() : null)
                .createdByEmail(p.getCreatedBy() != null ? p.getCreatedBy().getEmail() : null)
                .build();
    }
}