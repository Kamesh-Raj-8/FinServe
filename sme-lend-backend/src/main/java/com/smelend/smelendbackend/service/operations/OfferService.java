package com.smelend.smelendbackend.service.operations;

import com.smelend.smelendbackend.dto.operations.offer.CreateOfferRequest;
import com.smelend.smelendbackend.dto.operations.offer.OfferResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.LoanApplication;
import com.smelend.smelendbackend.entity.LoanProduct;
import com.smelend.smelendbackend.entity.Offer;
import com.smelend.smelendbackend.entity.enums.ApplicationStatus;
import com.smelend.smelendbackend.entity.enums.AuditAction;
import com.smelend.smelendbackend.entity.enums.OfferStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.repository.OfferRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import org.springframework.http.HttpStatus;
import com.smelend.smelendbackend.service.notification.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OfferService {

    private final OfferRepository offerRepo;
    private final LoanApplicationRepository appRepo;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    private final NotificationService notificationService;

    public OfferService(OfferRepository offerRepo,
                        LoanApplicationRepository appRepo,
                        CurrentUserService currentUserService,
                        AuditLogService auditLogService,
                          NotificationService notificationService) {
        this.offerRepo = offerRepo;
        this.appRepo = appRepo;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    /**
     * OPERATIONS creates offer only if application is UW_APPROVED.
     */
    public OfferResponse createOffer(Long applicationId, CreateOfferRequest req) {
        AppUser me = currentUserService.getCurrentUser();
        requireOpsOrAdmin(me);

        LoanApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (app.getStatus() != ApplicationStatus.UW_APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Offer can be created only for UW_APPROVED applications");
        }

        offerRepo.findByApplication_ApplicationId(applicationId).ifPresent(o -> {
            throw new ApiException(HttpStatus.CONFLICT, "Offer already exists for this application");
        });

        Offer saved = offerRepo.save(Offer.builder()
                .application(app)
                .sanctionedAmount(req.getSanctionedAmount())
                .interestRate(req.getInterestRate())
                .emiAmount(req.getEmiAmount())
                .validUntil(req.getValidUntil())
                .offerStatus(OfferStatus.OFFERED)
                .createdBy(me)
                .createdDate(LocalDateTime.now())
                .build());

        auditLogService.log(me, AuditAction.OFFER_CREATED, "OFFER", saved.getOfferId(), "Offer created for application " + applicationId);

        // Notify applicant
        if (app.getCreatedBy() != null) {
            notificationService.notifyOfferCreated(
                app.getCreatedBy().getEmail(), app.getCreatedBy().getFullName(),
                app.getApplicationId(), req.getSanctionedAmount().toPlainString());
        }

        // update app status to OFFERED
        app.setStatus(ApplicationStatus.OFFERED);
        appRepo.save(app);

        return toDto(saved);
    }

    /**
     * Applicant/Agent accepts offer.
     */
    public OfferResponse acceptOffer(Long offerId) {
        AppUser me = currentUserService.getCurrentUser();

        Offer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));

        LoanApplication app = offer.getApplication();
        ensureOwnerOrAdmin(app, me);

        if (offer.getOfferStatus() != OfferStatus.OFFERED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Offer is not in OFFERED status");
        }

        // expiry check
        if (offer.getValidUntil() != null && offer.getValidUntil().isBefore(java.time.LocalDate.now())) {
            offer.setOfferStatus(OfferStatus.EXPIRED);
            offerRepo.save(offer);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Offer already expired");
        }

        offer.setOfferStatus(OfferStatus.ACCEPTED);
        offerRepo.save(offer);

        auditLogService.log(me, AuditAction.OFFER_ACCEPTED, "OFFER", offer.getOfferId(), "Offer accepted");

        app.setStatus(ApplicationStatus.OFFER_ACCEPTED);
        appRepo.save(app);
        // Notify Operations: ready to disburse
        if (app.getCreatedBy() != null) {
            notificationService.notifyOfferAccepted(offer.getApplication().getApplicationId(),
                    app.getCreatedBy().getFullName());
        }

        return toDto(offer);
    }

    /**
     * Applicant/Agent rejects offer.
     */
    public OfferResponse rejectOffer(Long offerId) {
        AppUser me = currentUserService.getCurrentUser();

        Offer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));

        LoanApplication app = offer.getApplication();
        ensureOwnerOrAdmin(app, me);

        if (offer.getOfferStatus() != OfferStatus.OFFERED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Offer is not in OFFERED status");
        }

        offer.setOfferStatus(OfferStatus.REJECTED);
        offerRepo.save(offer);

        auditLogService.log(me, AuditAction.OFFER_REJECTED, "OFFER", offer.getOfferId(), "Offer rejected");

        app.setStatus(ApplicationStatus.OFFER_REJECTED);
        appRepo.save(app);
        if (app.getCreatedBy() != null) {
            notificationService.notifyOfferRejected(offer.getApplication().getApplicationId(),
                    app.getCreatedBy().getFullName());
        }

        return toDto(offer);
    }

    public OfferResponse get(Long offerId) {
        AppUser me = currentUserService.getCurrentUser();

        Offer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));

        LoanApplication app = offer.getApplication();
        boolean opsOrAdmin = isOpsOrAdmin(me);

        // Applicant/Agent can view only if owner. Ops/Admin can view all.
        if (!opsOrAdmin) {
            ensureOwnerOrAdmin(app, me);
        }

        return toDto(offer);
    }

    public List<OfferResponse> listMineOrAll() {
        AppUser me = currentUserService.getCurrentUser();
        if (isOpsOrAdmin(me)) {
            return offerRepo.findAll().stream().map(this::toDto).toList();
        }
        String role = me.getRole().getRoleName().name();
        // For APPLICANT: show offers where the application was created by me
        // OR where I am the linked KYC applicant
        return offerRepo.findAll().stream()
                .filter(o -> {
                    if (o.getApplication() == null) return false;
                    LoanApplication app = o.getApplication();
                    if (app.getCreatedBy() != null
                            && app.getCreatedBy().getUserId().equals(me.getUserId())) {
                        return true;
                    }
                    // Agent sees offers for SMEs they manage
                    if (role.equals("AGENT") && app.getSme() != null) {
                        return true; // Agents see all offer activity
                    }
                    return false;
                })
                .map(this::toDto)
                .toList();
    }

    /**
     * System-internal: auto-generates an offer for AUTO_APPROVED applications.
     * Skips the OPERATIONS role gate — called only by ApplicationService after
     * a high-score auto-decision (score ≥ 750). Idempotent: no-op if offer exists.
     *
     * Offer terms: requestedAmount @ product.baseInterestRate, standard EMI formula,
     * valid for 30 days. Sets application status to OFFERED on success.
     */
    public void createAutoOffer(LoanApplication app) {
        // Idempotent guard
        if (offerRepo.findByApplication_ApplicationId(app.getApplicationId()).isPresent()) {
            return;
        }

        LoanProduct product = app.getProduct();
        java.math.BigDecimal principal = app.getRequestedAmount();
        java.math.BigDecimal annualRate = product.getBaseInterestRate();
        int months = app.getTenorMonths();

        // Standard reducing-balance EMI: P·r·(1+r)^n / ((1+r)^n − 1)
        java.math.BigDecimal monthlyRate = annualRate
                .divide(java.math.BigDecimal.valueOf(1200), 10, java.math.RoundingMode.HALF_UP);

        java.math.BigDecimal emiAmount;
        if (monthlyRate.compareTo(java.math.BigDecimal.ZERO) == 0) {
            emiAmount = principal.divide(java.math.BigDecimal.valueOf(months), 2,
                    java.math.RoundingMode.HALF_UP);
        } else {
            java.math.BigDecimal onePlusR = java.math.BigDecimal.ONE.add(monthlyRate);
            java.math.BigDecimal power    = onePlusR.pow(months);
            emiAmount = principal.multiply(monthlyRate).multiply(power)
                    .divide(power.subtract(java.math.BigDecimal.ONE), 2,
                            java.math.RoundingMode.HALF_UP);
        }

        AppUser creator = app.getCreatedBy();
        Offer offer = offerRepo.save(Offer.builder()
                .application(app)
                .sanctionedAmount(principal)
                .interestRate(annualRate)
                .emiAmount(emiAmount)
                .validUntil(java.time.LocalDate.now().plusDays(30))
                .offerStatus(OfferStatus.OFFERED)
                .createdBy(creator)
                .createdDate(LocalDateTime.now())
                .build());

        app.setStatus(ApplicationStatus.OFFERED);
        appRepo.save(app);

        if (creator != null) {
            notificationService.notifyOfferCreated(
                    creator.getEmail(), creator.getFullName(),
                    app.getApplicationId(), principal.toPlainString());
            auditLogService.log(creator, AuditAction.OFFER_CREATED, "OFFER", offer.getOfferId(),
                    "Auto-offer generated for auto-approved application #" + app.getApplicationId());
        }
    }

    private void requireOpsOrAdmin(AppUser me) {
        String role = me.getRole().getRoleName().name();
        if (!(role.equals("OPERATIONS") || role.equals("ADMIN"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only OPERATIONS/ADMIN can create offers");
        }
    }

    private boolean isOpsOrAdmin(AppUser me) {
        String role = me.getRole().getRoleName().name();
        return role.equals("OPERATIONS") || role.equals("ADMIN");
    }

    private void ensureOwnerOrAdmin(LoanApplication app, AppUser me) {
        boolean owner = app.getCreatedBy() != null && app.getCreatedBy().getUserId().equals(me.getUserId());
        if (!owner && !currentUserService.isAdmin(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not allowed for this application");
        }
    }

    private OfferResponse toDto(Offer o) {
        return OfferResponse.builder()
                .offerId(o.getOfferId())
                .applicationId(o.getApplication() != null ? o.getApplication().getApplicationId() : null)
                .sanctionedAmount(o.getSanctionedAmount())
                .interestRate(o.getInterestRate())
                .emiAmount(o.getEmiAmount())
                .validUntil(o.getValidUntil())
                .offerStatus(o.getOfferStatus())
                .createdByUserId(o.getCreatedBy() != null ? o.getCreatedBy().getUserId() : null)
                .createdByEmail(o.getCreatedBy() != null ? o.getCreatedBy().getEmail() : null)
                .createdDate(o.getCreatedDate())
                .build();
    }
}