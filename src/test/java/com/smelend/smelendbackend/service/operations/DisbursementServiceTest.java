package com.smelend.smelendbackend.service.operations;

import com.smelend.smelendbackend.dto.operations.disbursement.DisburseRequest;
import com.smelend.smelendbackend.dto.operations.disbursement.DisbursementResponse;
import com.smelend.smelendbackend.dto.operations.disbursement.LoanAccountResponse;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.*;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.*;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import com.smelend.smelendbackend.service.servicing.EmiScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisbursementServiceTest {

    @Mock private DisbursementRepository disbRepo;
    @Mock private LoanAccountRepository loanRepo;
    @Mock private LoanApplicationRepository appRepo;
    @Mock private OfferRepository offerRepo;
    @Mock private CurrentUserService currentUserService;
    @Mock private EmiScheduleService emiScheduleService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private DisbursementService disbursementService;

    private AppUser opsUser;
    private LoanApplication application;

    @BeforeEach
    void setUp() {
        Role opsRole = new Role();
        opsRole.setRoleName(RoleName.OPERATIONS);

        opsUser = new AppUser();
        opsUser.setUserId(2L);
        opsUser.setRole(opsRole);

        application = new LoanApplication();
        application.setApplicationId(200L);
        application.setStatus(ApplicationStatus.OFFER_ACCEPTED);
        application.setTenorMonths(12);
    }

    @Test
    void disburse_Success() {
        DisburseRequest req = new DisburseRequest();
        req.setMode(DisbursementMode.NEFT);
        req.setDisbursementDate(LocalDate.now());

        Offer offer = new Offer();
        offer.setSanctionedAmount(new BigDecimal("25000"));
        offer.setInterestRate(new BigDecimal("14.0"));

        when(currentUserService.getCurrentUser()).thenReturn(opsUser);
        when(appRepo.findById(200L)).thenReturn(Optional.of(application));
        when(offerRepo.findByApplication_ApplicationId(200L)).thenReturn(Optional.of(offer));
        when(disbRepo.findByApplication_ApplicationId(200L)).thenReturn(Optional.empty());
        when(loanRepo.findByApplication_ApplicationId(200L)).thenReturn(Optional.empty());

        when(disbRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepo.save(any())).thenAnswer(i -> {
            LoanAccount l = i.getArgument(0);
            l.setLoanAccountId(5L);
            return l;
        });

        DisbursementResponse response = disbursementService.disburse(200L, req);

        assertNotNull(response);
        assertEquals(ApplicationStatus.DISBURSED, application.getStatus());
        verify(emiScheduleService).generateIfNotExists(anyLong());
        verify(auditLogService, atLeastOnce()).log(any(), any(), any(), any(), any());
    }

    @Test
    void getLoanAccount_Success() {
        LoanAccount loan = new LoanAccount();
        loan.setLoanAccountId(500L);
        loan.setAccountNumber("LA987654321");
        loan.setStatus(LoanAccountStatus.ACTIVE);

        when(loanRepo.findById(500L)).thenReturn(Optional.of(loan));

        LoanAccountResponse response = disbursementService.getLoanAccount(500L);

        assertNotNull(response);
        assertEquals("LA987654321", response.getAccountNumber());
    }

    @Test
    void disburse_WrongStatus_ThrowsException() {
        application.setStatus(ApplicationStatus.DRAFT); // Not accepted
        DisburseRequest req = new DisburseRequest();

        when(currentUserService.getCurrentUser()).thenReturn(opsUser);
        when(appRepo.findById(200L)).thenReturn(Optional.of(application));

        assertThrows(ApiException.class, () -> disbursementService.disburse(200L, req));
    }
}