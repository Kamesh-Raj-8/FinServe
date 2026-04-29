package com.smelend.smelendbackend.service.operations;

import com.smelend.smelendbackend.dto.operations.disbursement.DisburseRequest;
import com.smelend.smelendbackend.dto.operations.disbursement.DisbursementResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.LoanAccountResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.PendingDisbursementDto;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.*;
import com.smelend.smelendbackend.repository.*;
import com.smelend.smelendbackend.service.charge.ChargeService;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import com.smelend.smelendbackend.service.fee.FeeService;
import com.smelend.smelendbackend.service.notification.NotificationService;
import com.smelend.smelendbackend.service.servicing.EmiScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisbursementServiceTest {

    @Mock private DisbursementRepository disbRepo;
    @Mock private LoanAccountRepository loanRepo;
    @Mock private LoanApplicationRepository appRepo;
    @Mock private OfferRepository offerRepo;
    @Mock private DelinquencyRepository delinRepo;
    @Mock private CurrentUserService currentUserService;
    @Mock private EmiScheduleService emiScheduleService;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @Mock private FeeService feeService;
    @Mock private ChargeService chargeService;

    @InjectMocks
    private DisbursementService disbursementService;

    private AppUser opsUser;
    private LoanApplication application;
    private Offer offer;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRoleName(RoleName.OPERATIONS);

        opsUser = new AppUser();
        opsUser.setRole(role);

        application = new LoanApplication();
        application.setApplicationId(1L);
        application.setStatus(ApplicationStatus.OFFER_ACCEPTED);
        application.setProduct(LoanProduct.builder().productId(10L).build());

        offer = new Offer();
        offer.setApplication(application);
        offer.setSanctionedAmount(new BigDecimal("10000.00"));
        offer.setInterestRate(new BigDecimal("12.0"));
    }

    @Test
    void disburse() {
        DisburseRequest req = new DisburseRequest();
        req.setMode(DisbursementMode.NEFT);
        req.setDisbursementDate(LocalDate.now());

        when(currentUserService.getCurrentUser()).thenReturn(opsUser);
        when(appRepo.findById(1L)).thenReturn(Optional.of(application));
        when(offerRepo.findByApplication_ApplicationId(1L)).thenReturn(Optional.of(offer));
        when(feeService.calculateFees(anyLong(), any())).thenReturn(Collections.emptyList());
        when(disbRepo.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(loanRepo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        DisbursementResponse resp = disbursementService.disburse(1L, req);

        assertNotNull(resp);
        assertEquals(ApplicationStatus.DISBURSED, application.getStatus());
        verify(emiScheduleService).generateIfNotExists(any());
        verify(auditLogService, atLeastOnce()).log(any(), any(), any(), any(), any());
    }

    @Test
    void getLoanAccount() {
        LoanAccount la = LoanAccount.builder()
                .loanAccountId(500L)
                .accountNumber("LA-TEST-001")
                .application(application)
                .status(LoanAccountStatus.ACTIVE)
                .build();

        when(loanRepo.findById(500L)).thenReturn(Optional.of(la));

        LoanAccountResponse resp = disbursementService.getLoanAccount(500L);

        assertNotNull(resp);
        assertEquals("LA-TEST-001", resp.getAccountNumber());
    }

    @Test
    void listPendingDisbursements() {
        offer.setOfferStatus(OfferStatus.ACCEPTED);

        when(offerRepo.findAllForAcceptedApplications()).thenReturn(List.of(offer));

        List<PendingDisbursementDto> list = disbursementService.listPendingDisbursements();

        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
    }
}