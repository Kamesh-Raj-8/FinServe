package com.smelend.smelendbackend.service.kyc;

import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.KycRecord;
import com.smelend.smelendbackend.entity.LoanApplication;
import com.smelend.smelendbackend.entity.Sme;
import com.smelend.smelendbackend.entity.enums.ApplicationStatus;
import com.smelend.smelendbackend.entity.enums.KycStatus;
import com.smelend.smelendbackend.entity.enums.PartyType;

/**
 * Central test-data factory for KYC service tests
 */
class TestData {

    static AppUser applicantUser() {
        return TestBuilders.user(
                "APPLICANT",
                1L,
                "app@test.com"
        );
    }

    static AppUser agentUser() {
        return TestBuilders.user(
                "AGENT",
                2L,
                "agent@test.com"
        );
    }

    static Sme sme() {
        Sme s = new Sme();
        s.setSmeId(1L);
        return s;
    }

    static KycRecord pendingKyc(Sme sme) {
        return KycRecord.builder()
                .kycId(1L)
                .sme(sme)
                .partyType(PartyType.BUSINESS)
                .verificationStatus(KycStatus.PENDING)
                .notes(null)
                .verifiedBy(null)
                .verifiedDate(null)
                .build();
    }

    static LoanApplication draftApp(Sme sme) {
        LoanApplication a = new LoanApplication();
        a.setSme(sme);
        a.setStatus(ApplicationStatus.DRAFT);
        return a;
    }

    static LoanApplication kycPendingApp(Sme sme) {
        LoanApplication a = new LoanApplication();
        a.setSme(sme);
        a.setStatus(ApplicationStatus.KYC_PENDING);
        return a;
    }
}