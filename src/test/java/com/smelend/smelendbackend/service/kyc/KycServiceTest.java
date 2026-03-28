package com.smelend.smelendbackend.service.kyc;

import com.smelend.smelendbackend.dto.kyc.CreateKycRequest;
import com.smelend.smelendbackend.dto.kyc.KycActionRequest;
import com.smelend.smelendbackend.dto.kyc.KycResponse;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.*;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.KycRecordRepository;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.repository.SmeRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock
    private KycRecordRepository kycRepo;

    @Mock
    private SmeRepository smeRepo;

    @Mock
    private LoanApplicationRepository appRepo;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private KycService kycService;

    private AppUser applicantUser;
    private AppUser agentUser;
    private Sme sme;

    @BeforeEach
    void setUp() {
        applicantUser = TestData.applicantUser();
        agentUser = TestData.agentUser();
        sme = TestData.sme();
    }

    // ---------------- CREATE ----------------

    @Test
    void create_shouldCreatePendingKyc_andMoveDraftAppsToKycPending() {
        when(currentUserService.getCurrentUser()).thenReturn(applicantUser);
        when(smeRepo.findById(1L)).thenReturn(Optional.of(sme));
        when(kycRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        LoanApplication draftApp = TestData.draftApp(sme);
        when(appRepo.findAll()).thenReturn(List.of(draftApp));

        CreateKycRequest req = new CreateKycRequest();
        req.setSmeId(1L);
        req.setPartyType(PartyType.BUSINESS);

        KycResponse response = kycService.create(req);

        assertEquals(KycStatus.PENDING, response.getVerificationStatus());
        verify(appRepo).save(argThat(a -> a.getStatus() == ApplicationStatus.KYC_PENDING));
        verify(auditLogService).log(any(), eq(AuditAction.KYC_CREATED), any(), any(), any());
    }

    // ---------------- LIST ----------------

    @Test
    void listBySme_shouldReturnKycList() {
        when(currentUserService.getCurrentUser()).thenReturn(applicantUser);
        when(smeRepo.findById(1L)).thenReturn(Optional.of(sme));
        when(kycRepo.findBySme_SmeId(1L))
                .thenReturn(List.of(TestData.pendingKyc(sme)));

        List<KycResponse> result = kycService.listBySme(1L);

        assertEquals(1, result.size());
        assertEquals(KycStatus.PENDING, result.get(0).getVerificationStatus());
    }

    // ---------------- VERIFY ----------------

    @Test
    void verify_shouldVerifyKyc_andMoveAppsToReadyToSubmit_whenBusiness() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser);

        KycRecord kyc = TestData.pendingKyc(sme);
        when(kycRepo.findById(1L)).thenReturn(Optional.of(kyc));
        when(kycRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        LoanApplication app = TestData.kycPendingApp(sme);
        when(appRepo.findAll()).thenReturn(List.of(app));

        KycActionRequest req = new KycActionRequest();
        req.setNotes("verified");

        KycResponse response = kycService.verify(1L, req);

        assertEquals(KycStatus.VERIFIED, response.getVerificationStatus());
        assertEquals(agentUser.getUserId(), response.getVerifiedByUserId());
        assertEquals(LocalDate.now(), response.getVerifiedDate());
        verify(appRepo).save(argThat(a -> a.getStatus() == ApplicationStatus.READY_TO_SUBMIT));
    }

    // ---------------- REJECT ----------------

    @Test
    void reject_shouldRejectKyc() {
        when(currentUserService.getCurrentUser()).thenReturn(agentUser);

        KycRecord kyc = TestData.pendingKyc(sme);
        when(kycRepo.findById(1L)).thenReturn(Optional.of(kyc));
        when(kycRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        KycActionRequest req = new KycActionRequest();
        req.setNotes("documents invalid");

        KycResponse response = kycService.reject(1L, req);

        assertEquals(KycStatus.REJECTED, response.getVerificationStatus());
        assertEquals("documents invalid", response.getNotes());
        verify(auditLogService).log(any(), eq(AuditAction.KYC_REJECTED), any(), any(), any());
    }

    // ---------------- SECURITY ----------------

    @Test
    void verify_shouldFail_forApplicantRole() {
        when(currentUserService.getCurrentUser()).thenReturn(applicantUser);

        ApiException ex = assertThrows(ApiException.class,
                () -> kycService.verify(1L, new KycActionRequest()));

        assertEquals(403, ex.getStatus().value());
    }
}
