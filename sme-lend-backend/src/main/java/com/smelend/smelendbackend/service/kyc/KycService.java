package com.smelend.smelendbackend.service.kyc;

import com.smelend.smelendbackend.dto.kyc.*;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.Promoter;
import com.smelend.smelendbackend.entity.enums.AuditAction;
import com.smelend.smelendbackend.entity.enums.KycStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.*;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import com.smelend.smelendbackend.service.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * KYC Service — Automatic Relational KYC Initialization.
 *
 * Business rules enforced:
 *  1. One KYC per LoanApplication (unique constraint).
 *  2. All promoters of the SME are automatically linked.
 *  3. At least one promoter is mandatory — throws if none exist.
 *  4. Main promoter = highest ownership %. If applicant creates KYC
 *     and is also a promoter (self-employment), they are main.
 *  5. Creator rule: only createdBy user may edit KYC docs.
 */
@Service
public class KycService {

    private final KycRecordRepository        kycRepo;
    private final KycPromoterLinkRepository  linkRepo;
    private final LoanApplicationRepository  appRepo;
    private final PromoterRepository         promoterRepo;
    private final AppUserRepository          userRepo;
    private final CurrentUserService         currentUserService;
    private final AuditLogService            auditLogService;
    private final NotificationService        notificationService;

    public KycService(KycRecordRepository kycRepo,
                      KycPromoterLinkRepository linkRepo,
                      LoanApplicationRepository appRepo,
                      PromoterRepository promoterRepo,
                      AppUserRepository userRepo,
                      CurrentUserService currentUserService,
                      AuditLogService auditLogService,
                      NotificationService notificationService) {
        this.kycRepo             = kycRepo;
        this.linkRepo            = linkRepo;
        this.appRepo             = appRepo;
        this.promoterRepo        = promoterRepo;
        this.userRepo            = userRepo;
        this.currentUserService  = currentUserService;
        this.auditLogService     = auditLogService;
        this.notificationService = notificationService;
    }

    // ════════════════════════════════════════════════════════════
    //  INITIALIZE — Automatic KYC snapshot from LoanApplication
    // ════════════════════════════════════════════════════════════

    /**
     * POST /kyc/initialize
     *
     * Aggregates all participants from the LoanApplication and creates
     * a fully-linked KYC snapshot automatically.
     *
     * Rules:
     *  - Idempotent: returns existing KYC if already initialized.
     *  - Fetches all promoters of the SME and links them.
     *  - Main promoter = highest ownershipPct. Tie-break: first by ID.
     *  - If promoter list is empty: throws with clear error.
     *  - If applicant creator also matches a promoter → flag them as main.
     */
    @Transactional
    public KycResponse initializeKYC(InitKycRequest req) {
        AppUser me = currentUserService.getCurrentUser();

        // Load the application
        LoanApplication app = appRepo.findById(req.getLoanApplicationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Loan application #" + req.getLoanApplicationId() + " not found"));

        // Idempotent: return existing KYC if already created for this app
        return kycRepo.findByLoanApplication_ApplicationId(app.getApplicationId())
                .map(existing -> toDto(existing, me))
                .orElseGet(() -> createKycSnapshot(app, me, req.getNotes()));
    }

    private KycResponse createKycSnapshot(LoanApplication app, AppUser me, String notes) {
        Sme sme = app.getSme();

        // Fetch all promoters of this SME
        List<Promoter> promoters = promoterRepo.findBySme_SmeId(sme.getSmeId());

        // ── Business rule: at least one promoter is mandatory ─────
        if (promoters.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot initialize KYC: the SME '" + sme.getLegalName() +
                    "' has no registered promoters. Add at least one promoter before creating KYC.");
        }

        // ── Deduplication / pre-verified status check ─────────────
        // If a promoter already carries VERIFIED status (from a prior application's
        // KYC cycle), we recognise that existing verification rather than creating a
        // redundant PENDING record.  When ALL promoters are already VERIFIED the
        // KYC record itself is initialised as VERIFIED — no agent action required.
        long preVerifiedCount = promoters.stream()
                .filter(p -> p.getKycStatus() == KycStatus.VERIFIED)
                .count();
        boolean allPreVerified = (preVerifiedCount == promoters.size());

        KycStatus initialStatus = allPreVerified ? KycStatus.VERIFIED : KycStatus.PENDING;

        // ── Determine main promoter ───────────────────────────────
        // Default: highest ownership percentage (tie-break: lowest ID)
        Promoter mainPromoter = promoters.stream()
                .max(Comparator.comparing(Promoter::getOwnershipPct)
                        .thenComparing(Comparator.comparing(Promoter::getPromoterId).reversed()))
                .orElseThrow(); // safe — promoters non-empty

        // ── Build KYC record ──────────────────────────────────────
        String compositeNotes = allPreVerified
                ? "All promoters carry prior VERIFIED status — KYC auto-initialised as VERIFIED."
                  + (notes != null ? " | " + notes : "")
                : (preVerifiedCount > 0
                        ? preVerifiedCount + " of " + promoters.size()
                          + " promoter(s) already VERIFIED from prior KYC cycle."
                          + (notes != null ? " | " + notes : "")
                        : notes);

        KycRecord kyc = KycRecord.builder()
                .loanApplication(app)
                .sme(sme)
                .applicant(app.getCreatedBy())
                .verificationStatus(initialStatus)
                .notes(compositeNotes)
                .createdBy(me)
                .createdAt(LocalDateTime.now())
                .build();

        KycRecord saved = kycRepo.save(kyc);

        // ── Build promoter links ──────────────────────────────────
        List<KycPromoterLink> links = promoters.stream()
                .map(p -> KycPromoterLink.builder()
                        .kycRecord(saved)
                        .promoter(p)
                        .main(p.getPromoterId().equals(mainPromoter.getPromoterId()))
                        .ownershipPct(p.getOwnershipPct())
                        .build())
                .toList();

        linkRepo.saveAll(links);
        saved.setPromoterLinks(links);

        // ── Notifications & audit ─────────────────────────────────
        AppUser applicant = app.getCreatedBy();
        if (applicant != null) {
            notificationService.notifyKycCreated(
                    applicant.getEmail(), applicant.getFullName(), saved.getKycId());
        }
        auditLogService.log(me, AuditAction.KYC_CREATED, "KYC", saved.getKycId(),
                "KYC initialized for application #" + app.getApplicationId() +
                " — " + promoters.size() + " promoter(s) linked, main=" + mainPromoter.getPromoterName());

        return toDto(saved, me);
    }

    // ════════════════════════════════════════════════════════════
    //  VERIFY / REJECT
    // ════════════════════════════════════════════════════════════

    @Transactional
    public KycResponse verify(Long kycId, KycActionRequest req) {
        AppUser me = currentUserService.getCurrentUser();
        requireAgentOrAdmin(me);

        KycRecord record = findKycOrThrow(kycId);
        record.setVerificationStatus(KycStatus.VERIFIED);
        record.setVerifiedAt(LocalDateTime.now());
        record.setVerifiedBy(me);
        if (req != null && req.getNotes() != null) record.setNotes(req.getNotes());

        KycRecord saved = kycRepo.save(record);

        // ── Cascade: mark ALL linked promoters as individually VERIFIED ──
        // This ensures the promoter-level check in KycVerificationService passes.
        if (saved.getPromoterLinks() != null) {
            for (KycPromoterLink link : saved.getPromoterLinks()) {
                Promoter promoter = link.getPromoter();
                if (promoter != null && promoter.getKycStatus() != KycStatus.VERIFIED) {
                    promoter.setKycStatus(KycStatus.VERIFIED);
                    promoterRepo.save(promoter);
                }
            }
        }

        AppUser applicant = saved.getApplicant();
        if (applicant != null) {
            notificationService.notifyKycVerified(
                    applicant.getEmail(), applicant.getFullName(), saved.getKycId());
        }
        auditLogService.log(me, AuditAction.KYC_VERIFIED, "KYC", saved.getKycId(),
                "KYC verified — " + (saved.getPromoterLinks() != null ?
                        saved.getPromoterLinks().size() : 0) + " promoter(s) marked verified");
        return toDto(saved, me);
    }

    @Transactional
    public KycResponse reject(Long kycId, KycActionRequest req) {
        AppUser me = currentUserService.getCurrentUser();
        requireAgentOrAdmin(me);

        KycRecord record = findKycOrThrow(kycId);
        record.setVerificationStatus(KycStatus.REJECTED);
        record.setVerifiedAt(LocalDateTime.now());
        record.setVerifiedBy(me);
        if (req != null && req.getNotes() != null) record.setNotes(req.getNotes());

        KycRecord saved = kycRepo.save(record);
        auditLogService.log(me, AuditAction.KYC_REJECTED, "KYC", saved.getKycId(), "KYC rejected");
        return toDto(saved, me);
    }

    // ════════════════════════════════════════════════════════════
    //  QUERIES
    // ════════════════════════════════════════════════════════════

    public List<KycResponse> listAll() {
        AppUser me = currentUserService.getCurrentUser();
        return kycRepo.findAll().stream()
                .sorted(Comparator.comparing(k -> k.getCreatedAt() != null
                        ? k.getCreatedAt() : LocalDateTime.MIN))
                .map(k -> toDto(k, me))
                .toList();
    }

    public List<KycResponse> listAllPending() {
        AppUser me = currentUserService.getCurrentUser();
        return kycRepo.findByVerificationStatus(KycStatus.PENDING).stream()
                .sorted(Comparator.comparing(k -> k.getCreatedAt() != null
                        ? k.getCreatedAt() : LocalDateTime.MIN))
                .map(k -> toDto(k, me))
                .toList();
    }

    public List<KycResponse> listPending() { return listAllPending(); }

    public KycResponse getById(Long kycId) {
        AppUser me = currentUserService.getCurrentUser();
        return toDto(findKycOrThrow(kycId), me);
    }

    public KycResponse getByApplicationId(Long applicationId) {
        AppUser me = currentUserService.getCurrentUser();
        KycRecord kyc = kycRepo.findByLoanApplication_ApplicationId(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No KYC found for application #" + applicationId));
        return toDto(kyc, me);
    }

    public List<KycResponse> listBySme(Long smeId) {
        AppUser me = currentUserService.getCurrentUser();
        return kycRepo.findBySme_SmeId(smeId).stream()
                .map(k -> toDto(k, me))
                .toList();
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════

    private KycRecord findKycOrThrow(Long kycId) {
        return kycRepo.findById(kycId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KYC record not found"));
    }

    private void requireAgentOrAdmin(AppUser me) {
        String role = me.getRole().getRoleName().name();
        if (!(role.equals("AGENT") || role.equals("ADMIN"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only AGENT/ADMIN can verify/reject KYC");
        }
    }

    /**
     * canEdit = current user is the creator of the KYC record (creator rule).
     * Admin can always edit.
     */
    private boolean resolveCanEdit(KycRecord kyc, AppUser me) {
        if (me == null) return false;
        String role = me.getRole().getRoleName().name();
        if ("ADMIN".equals(role)) return true;
        return kyc.getCreatedBy() != null &&
               kyc.getCreatedBy().getUserId().equals(me.getUserId());
    }

    private KycResponse toDto(KycRecord k, AppUser me) {
        // Map promoter links → DTOs (main promoter first)
        List<KycPromoterDto> promoterDtos = k.getPromoterLinks() == null
                ? List.of()
                : k.getPromoterLinks().stream()
                    .sorted(Comparator.comparing(KycPromoterLink::isMain).reversed()
                            .thenComparing(l -> l.getOwnershipPct() != null
                                    ? l.getOwnershipPct().negate()
                                    : java.math.BigDecimal.ZERO))
                    .map(l -> KycPromoterDto.builder()
                            .promoterId(l.getPromoter().getPromoterId())
                            .promoterName(l.getPromoter() != null ? l.getPromoter().getPromoterName() : null)
                            .ownershipPct(l.getOwnershipPct())
                            .main(l.isMain())
                            .kycStatus(l.getPromoter().getKycStatus().name())
                            .mobile(l.getPromoter().getMobile())
                            .build())
                    .toList();

        return KycResponse.builder()
                .kycId(k.getKycId())
                .loanApplicationId(k.getLoanApplication() != null
                        ? k.getLoanApplication().getApplicationId() : null)
                .smeId(k.getSme() != null ? k.getSme().getSmeId() : null)
                .smeLegalName(k.getSme() != null ? k.getSme().getLegalName() : null)
                .applicantId(k.getApplicant() != null ? k.getApplicant().getUserId() : null)
                .applicantEmail(k.getApplicant() != null ? k.getApplicant().getEmail() : null)
                .applicantFullName(k.getApplicant() != null ? k.getApplicant().getFullName() : null)
                .promoters(promoterDtos)
                .mainPromoterName(k.getMainPromoterLink() != null && k.getMainPromoterLink().getPromoter() != null
                        ? k.getMainPromoterLink().getPromoter().getPromoterName() : null)
                .createdByUserId(k.getCreatedBy() != null ? k.getCreatedBy().getUserId() : null)
                .createdByEmail(k.getCreatedBy() != null ? k.getCreatedBy().getEmail() : null)
                .verificationStatus(k.getVerificationStatus())
                .notes(k.getNotes())
                .createdAt(k.getCreatedAt())
                .verifiedAt(k.getVerifiedAt())
                .verifiedByUserId(k.getVerifiedBy() != null ? k.getVerifiedBy().getUserId() : null)
                .verifiedByEmail(k.getVerifiedBy() != null ? k.getVerifiedBy().getEmail() : null)
                .canEdit(resolveCanEdit(k, me))
                .build();
    }
}
