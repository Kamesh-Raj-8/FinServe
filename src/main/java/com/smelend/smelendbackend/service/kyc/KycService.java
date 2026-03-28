package com.smelend.smelendbackend.service.kyc;

import com.smelend.smelendbackend.dto.kyc.CreateKycRequest;
import com.smelend.smelendbackend.dto.kyc.KycActionRequest;
import com.smelend.smelendbackend.dto.kyc.KycResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.KycRecord;
import com.smelend.smelendbackend.entity.LoanApplication;
import com.smelend.smelendbackend.entity.Sme;
import com.smelend.smelendbackend.entity.enums.ApplicationStatus;
import com.smelend.smelendbackend.entity.enums.AuditAction;
import com.smelend.smelendbackend.entity.enums.KycStatus;
import com.smelend.smelendbackend.entity.enums.PartyType;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.KycRecordRepository;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.repository.SmeRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class KycService {

    private final KycRecordRepository kycRepo;
    private final SmeRepository smeRepo;
    private final LoanApplicationRepository appRepo;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public KycService(KycRecordRepository kycRepo,
                      SmeRepository smeRepo,
                      LoanApplicationRepository appRepo,
                      CurrentUserService currentUserService,
                      AuditLogService auditLogService) {
        this.kycRepo = kycRepo;
        this.smeRepo = smeRepo;
        this.appRepo = appRepo;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    /**
     * Applicant/Agent/Admin creates a KYC record (starts as PENDING).
     * For demo flow, any authenticated onboarding actor can create KYC for an SME.
     */
    public KycResponse create(CreateKycRequest req) {
        AppUser me = currentUserService.getCurrentUser();
        requireApplicantAgentOrAdmin(me);

        Sme sme = smeRepo.findById(req.getSmeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SME not found"));

        KycRecord saved = kycRepo.save(KycRecord.builder()
                .sme(sme)
                .partyType(req.getPartyType())
                .verificationStatus(KycStatus.PENDING)
                .notes(null)
                .verifiedBy(null)
                .verifiedDate(null)
                .build());

        auditLogService.log(
                me,
                AuditAction.KYC_CREATED,
                "KYC",
                saved.getKycId(),
                "KYC created for SME " + sme.getSmeId()
        );

        // If any DRAFT apps exist for this SME, move them to KYC_PENDING
        markDraftAppsAsKycPending(sme.getSmeId());

        return toDto(saved);
    }

    /**
     * Applicant/Agent/Admin can view KYC list for SME.
     */
    public List<KycResponse> listBySme(Long smeId) {
        AppUser me = currentUserService.getCurrentUser();
        requireApplicantAgentOrAdmin(me);

        Sme sme = smeRepo.findById(smeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SME not found"));

        return kycRepo.findBySme_SmeId(sme.getSmeId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Only AGENT/ADMIN verifies KYC.
     */
    public KycResponse verify(Long kycId, KycActionRequest req) {
        AppUser me = currentUserService.getCurrentUser();
        requireAgentOrAdmin(me);

        KycRecord kyc = kycRepo.findById(kycId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KYC record not found"));

        kyc.setVerificationStatus(KycStatus.VERIFIED);
        kyc.setVerifiedBy(me);
        kyc.setVerifiedDate(LocalDate.now());
        kyc.setNotes(req != null ? req.getNotes() : null);

        KycRecord saved = kycRepo.save(kyc);

        auditLogService.log(
                me,
                AuditAction.KYC_VERIFIED,
                "KYC",
                saved.getKycId(),
                "KYC verified"
        );

        // When BUSINESS KYC is verified, move SME applications to READY_TO_SUBMIT
        if (saved.getPartyType() == PartyType.BUSINESS) {
            markAppsReadyToSubmit(saved.getSme().getSmeId());
        }

        return toDto(saved);
    }

    /**
     * Only AGENT/ADMIN rejects KYC.
     */
    public KycResponse reject(Long kycId, KycActionRequest req) {
        AppUser me = currentUserService.getCurrentUser();
        requireAgentOrAdmin(me);

        KycRecord kyc = kycRepo.findById(kycId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KYC record not found"));

        kyc.setVerificationStatus(KycStatus.REJECTED);
        kyc.setVerifiedBy(me);
        kyc.setVerifiedDate(LocalDate.now());
        kyc.setNotes(req != null ? req.getNotes() : null);

        KycRecord saved = kycRepo.save(kyc);

        auditLogService.log(
                me,
                AuditAction.KYC_REJECTED,
                "KYC",
                saved.getKycId(),
                "KYC rejected"
        );

        return toDto(saved);
    }

    private void requireApplicantAgentOrAdmin(AppUser me) {
        String role = me.getRole().getRoleName().name();
        if (!(role.equals("APPLICANT") || role.equals("AGENT") || role.equals("ADMIN"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only APPLICANT/AGENT/ADMIN can access KYC creation/view");
        }
    }

    private void requireAgentOrAdmin(AppUser me) {
        String role = me.getRole().getRoleName().name();
        if (!(role.equals("AGENT") || role.equals("ADMIN"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only AGENT/ADMIN can perform verification");
        }
    }

    private void markDraftAppsAsKycPending(Long smeId) {
        List<LoanApplication> apps = appRepo.findAll().stream()
                .filter(a -> a.getSme() != null && a.getSme().getSmeId().equals(smeId))
                .filter(a -> a.getStatus() == ApplicationStatus.DRAFT)
                .toList();

        for (LoanApplication a : apps) {
            a.setStatus(ApplicationStatus.KYC_PENDING);
            appRepo.save(a);
        }
    }

    private void markAppsReadyToSubmit(Long smeId) {
        List<LoanApplication> apps = appRepo.findAll().stream()
                .filter(a -> a.getSme() != null && a.getSme().getSmeId().equals(smeId))
                .filter(a -> a.getStatus() == ApplicationStatus.KYC_PENDING || a.getStatus() == ApplicationStatus.DRAFT)
                .toList();

        for (LoanApplication a : apps) {
            a.setStatus(ApplicationStatus.READY_TO_SUBMIT);
            appRepo.save(a);
        }
    }

    private KycResponse toDto(KycRecord k) {
        return KycResponse.builder()
                .kycId(k.getKycId())
                .smeId(k.getSme() != null ? k.getSme().getSmeId() : null)
                .partyType(k.getPartyType())
                .verificationStatus(k.getVerificationStatus())
                .verifiedDate(k.getVerifiedDate())
                .verifiedByUserId(k.getVerifiedBy() != null ? k.getVerifiedBy().getUserId() : null)
                .verifiedByEmail(k.getVerifiedBy() != null ? k.getVerifiedBy().getEmail() : null)
                .notes(k.getNotes())
                .build();
    }
}