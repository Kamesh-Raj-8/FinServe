package com.smelend.smelendbackend.service.kyc;

import com.smelend.smelendbackend.entity.KycPromoterLink;
import com.smelend.smelendbackend.entity.KycRecord;
import com.smelend.smelendbackend.entity.LoanApplication;
import com.smelend.smelendbackend.entity.Promoter;
import com.smelend.smelendbackend.entity.enums.KycStatus;
import com.smelend.smelendbackend.repository.KycPromoterLinkRepository;
import com.smelend.smelendbackend.repository.KycRecordRepository;
import com.smelend.smelendbackend.repository.PromoterRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Comprehensive KYC readiness check for loan application submission.
 *
 * Rules enforced:
 *  1. A KYC record must exist and its verificationStatus = VERIFIED.
 *     (This KYC record covers the SME and the Applicant identity as a unit.)
 *  2. EVERY promoter linked in KycPromoterLink must have kycStatus = VERIFIED
 *     on their individual Promoter record.
 *  3. If no KYC record is linked to the application (legacy), check SME-level KYC.
 *  4. At least ONE promoter must be present and verified.
 */
@Service
public class KycVerificationService {

    private final KycRecordRepository         kycRepo;
    private final KycPromoterLinkRepository   linkRepo;
    private final PromoterRepository          promoterRepo;

    public KycVerificationService(KycRecordRepository kycRepo,
                                   KycPromoterLinkRepository linkRepo,
                                   PromoterRepository promoterRepo) {
        this.kycRepo      = kycRepo;
        this.linkRepo     = linkRepo;
        this.promoterRepo = promoterRepo;
    }

    /**
     * Result object describing exactly what is and is not verified.
     * Used both as a gate and to produce a descriptive error message.
     */
    public record KycReadinessResult(boolean ready, List<String> failures) {
        public static KycReadinessResult ok() {
            return new KycReadinessResult(true, List.of());
        }
        public static KycReadinessResult fail(List<String> failures) {
            return new KycReadinessResult(false, failures);
        }
    }

    /**
     * Full KYC readiness check for a loan application.
     *
     * @param app the application being submitted
     * @return KycReadinessResult — ready=true means all checks pass
     */
    public KycReadinessResult checkAll(LoanApplication app) {
        List<String> failures = new ArrayList<>();
        Long smeId = app.getSme() != null ? app.getSme().getSmeId() : null;

        // ── Check 1: KYC Record must exist and be VERIFIED ────────────
        Optional<KycRecord> kycOpt = kycRepo
                .findByLoanApplication_ApplicationId(app.getApplicationId());

        if (kycOpt.isEmpty()) {
            // Fall back to SME-level KYC (legacy path before application-linked KYC)
            boolean smeKycVerified = smeId != null &&
                    kycRepo.existsBySme_SmeIdAndVerificationStatus(smeId, KycStatus.VERIFIED);

            if (!smeKycVerified) {
                failures.add("KYC record not found for this application. " +
                             "Please initialize and verify KYC first.");
                // No further checks possible without a KYC record
                return KycReadinessResult.fail(failures);
            }

            // Legacy path: at least one KYC verified at SME level — now check promoters
            checkPromotersAtSmeLevel(smeId, failures);
            return failures.isEmpty() ? KycReadinessResult.ok() : KycReadinessResult.fail(failures);
        }

        KycRecord kyc = kycOpt.get();

        // ── Check 2: KYC record overall status ────────────────────────
        if (kyc.getVerificationStatus() == KycStatus.REJECTED) {
            failures.add("KYC has been REJECTED (KYC #" + kyc.getKycId() + "). " +
                         "Please re-submit and get KYC verified.");
        } else if (kyc.getVerificationStatus() != KycStatus.VERIFIED) {
            failures.add("KYC record (KYC #" + kyc.getKycId() + ") is still PENDING verification. " +
                         "An Agent must verify the KYC before submission.");
        }

        // ── Check 3: SME KYC identity verified ────────────────────────
        // The KYC record covers the SME — its verificationStatus VERIFIED means SME is approved.
        // We already checked this above via verificationStatus.

        // ── Check 4: Applicant identity verified ──────────────────────
        // The KYC record's 'applicant' field is the primary AppUser.
        // The overall KYC verification by an agent implies applicant details were checked.
        // Additional check: applicant must be linked in the KYC record.
        if (kyc.getApplicant() == null) {
            failures.add("KYC record has no applicant linked. " +
                         "Re-initialize KYC to auto-link the applicant.");
        }

        // ── Check 5: ALL promoters must have kycStatus = VERIFIED ──────
        List<KycPromoterLink> promoterLinks = linkRepo.findByKycRecord_KycId(kyc.getKycId());

        if (promoterLinks.isEmpty()) {
            failures.add("No promoters are linked in the KYC record. " +
                         "At least one promoter must be registered and verified.");
        } else {
            List<KycPromoterLink> unverified = linkRepo
                    .findUnverifiedPromotersByKyc(kyc.getKycId());

            for (KycPromoterLink link : unverified) {
                Promoter p = link.getPromoter();
                String who = link.isMain() ? "Main Promoter" : "Promoter";
                String name = p.getPromoterName() != null ? p.getPromoterName() : "#" + p.getPromoterId();
                String status = p.getKycStatus() != null ? p.getKycStatus().name() : "UNKNOWN";
                failures.add(who + " \"" + name + "\" KYC is " + status +
                             " (Promoter ID #" + p.getPromoterId() + "). " +
                             "All promoters must be individually verified.");
            }
        }

        return failures.isEmpty() ? KycReadinessResult.ok() : KycReadinessResult.fail(failures);
    }

    /** Legacy SME-level promoter check when no application-linked KYC exists */
    private void checkPromotersAtSmeLevel(Long smeId, List<String> failures) {
        if (smeId == null) {
            failures.add("SME not linked to application.");
            return;
        }
        long total = promoterRepo.countBySme_SmeId(smeId);
        if (total == 0) {
            failures.add("No promoters registered for this SME. " +
                         "At least one promoter must be added and verified.");
            return;
        }
        List<Promoter> unverified = promoterRepo.findUnverifiedBySme(smeId);
        for (Promoter p : unverified) {
            String name = p.getPromoterName() != null ? p.getPromoterName() : "#" + p.getPromoterId();
            String status = p.getKycStatus() != null ? p.getKycStatus().name() : "UNKNOWN";
            failures.add("Promoter \"" + name + "\" KYC is " + status +
                         " (Promoter ID #" + p.getPromoterId() + "). " +
                         "All promoters must be verified.");
        }
    }
}
