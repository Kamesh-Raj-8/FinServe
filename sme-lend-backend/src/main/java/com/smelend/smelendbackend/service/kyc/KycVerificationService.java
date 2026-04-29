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

    public record KycReadinessResult(boolean ready, List<String> failures) {
        public static KycReadinessResult ok() {
            return new KycReadinessResult(true, List.of());
        }
        public static KycReadinessResult fail(List<String> failures) {
            return new KycReadinessResult(false, failures);
        }
    }

    public KycReadinessResult checkAll(LoanApplication app) {
        List<String> failures = new ArrayList<>();
        Long smeId = app.getSme() != null ? app.getSme().getSmeId() : null;

        Optional<KycRecord> kycOpt = kycRepo
                .findByLoanApplication_ApplicationId(app.getApplicationId());

        if (kycOpt.isEmpty()) {
            boolean smeKycVerified = smeId != null &&
                    kycRepo.existsBySme_SmeIdAndVerificationStatus(smeId, KycStatus.VERIFIED);

            if (!smeKycVerified) {
                failures.add("KYC record not found for this application. " +
                             "Please initialize and verify KYC first.");
                return KycReadinessResult.fail(failures);
            }

            checkPromotersAtSmeLevel(smeId, failures);
            return failures.isEmpty() ? KycReadinessResult.ok() : KycReadinessResult.fail(failures);
        }

        KycRecord kyc = kycOpt.get();

        if (kyc.getVerificationStatus() == KycStatus.REJECTED) {
            failures.add("KYC has been REJECTED (KYC #" + kyc.getKycId() + "). " +
                         "Please re-submit and get KYC verified.");
        } else if (kyc.getVerificationStatus() != KycStatus.VERIFIED) {
            failures.add("KYC record (KYC #" + kyc.getKycId() + ") is still PENDING verification. " +
                         "An Agent must verify the KYC before submission.");
        }

        if (kyc.getApplicant() == null) {
            failures.add("KYC record has no applicant linked. " +
                         "Re-initialize KYC to auto-link the applicant.");
        }

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
