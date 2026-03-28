package com.smelend.smelendbackend.service.operations;

import com.smelend.smelendbackend.dto.operations.offer.CreateOfferRequest;
import com.smelend.smelendbackend.dto.operations.offer.OfferResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.LoanApplication;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OfferService {

    private final OfferRepository offerRepo;
    private final LoanApplicationRepository appRepo;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public OfferService(OfferRepository offerRepo,
                        LoanApplicationRepository appRepo,
                        CurrentUserService currentUserService,
                        AuditLogService auditLogService) {
        this.offerRepo = offerRepo;
        this.appRepo = appRepo;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
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
        // applicant/agent: list offers for MY applications (simple filter)
        return offerRepo.findAll().stream()
                .filter(o -> o.getApplication() != null
                        && o.getApplication().getCreatedBy() != null
                        && o.getApplication().getCreatedBy().getUserId().equals(me.getUserId()))
                .map(this::toDto)
                .toList();
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