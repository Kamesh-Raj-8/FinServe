package com.smelend.smelendbackend.service.application;

import com.smelend.smelendbackend.dto.application.ApplicationResponse;
import com.smelend.smelendbackend.dto.application.CreateApplicationRequest;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.ApplicationStatus;
import com.smelend.smelendbackend.entity.enums.AuditAction;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.repository.LoanProductRepository;
import com.smelend.smelendbackend.repository.SmeRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationServiceTest {

    private LoanApplicationRepository appRepo;
    private SmeRepository smeRepo;
    private LoanProductRepository productRepo;
    private CurrentUserService currentUserService;
    private AuditLogService auditLogService;
    private ApplicationService service;
    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        appRepo = mock(LoanApplicationRepository.class);
        smeRepo = mock(SmeRepository.class);
        productRepo = mock(LoanProductRepository.class);
        currentUserService = mock(CurrentUserService.class);
        auditLogService = mock(AuditLogService.class);

        service = new ApplicationService(appRepo, smeRepo, productRepo, currentUserService, auditLogService);

        mockUser = AppUser.builder().userId(1L).email("test@example.com").build();
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);
    }
    @Test
    void create_success() {
        // Arrange
        Sme sme = Sme.builder().smeId(10L).legalName("Test SME").createdBy(mockUser).build();
        LoanProduct product = LoanProduct.builder().productId(20L).productName("Loan Product").build();

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .smeId(10L)
                .productId(20L)
                .requestedAmount(BigDecimal.valueOf(100000.0))
                .tenorMonths(12)
                .purposeNote("Expansion")
                .build();
        when(smeRepo.findById(10L)).thenReturn(Optional.of(sme));
        when(productRepo.findById(20L)).thenReturn(Optional.of(product));
        when(appRepo.save(any())).thenAnswer(invocation -> {
            LoanApplication app = invocation.getArgument(0);
            app.setApplicationId(99L);
            return app;
        });
        ApplicationResponse response = service.create(req);
        assertNotNull(response);
        assertEquals(99L, response.getApplicationId());
        assertEquals(ApplicationStatus.DRAFT, response.getStatus());
        verify(auditLogService).log(eq(mockUser), eq(AuditAction.APPLICATION_CREATED),
                eq("APPLICATION"), eq(99L), contains("SME 10"));
    }

    @Test
    void create_forbiddenWhenNotOwner() {
        Sme sme = Sme.builder().smeId(10L).legalName("Test SME")
                .createdBy(AppUser.builder().userId(2L).build()).build();

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .smeId(10L).productId(20L).build();

        when(smeRepo.findById(10L)).thenReturn(Optional.of(sme));

        assertThrows(ApiException.class, () -> service.create(req));
    }

    @Test
    void submit_success() {
        LoanApplication app = LoanApplication.builder()
                .applicationId(50L)
                .status(ApplicationStatus.DRAFT)
                .createdBy(mockUser)
                .build();

        when(appRepo.findById(50L)).thenReturn(Optional.of(app));
        when(appRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationResponse response = service.submit(50L);

        assertEquals(ApplicationStatus.ROUTED_TO_UW, response.getStatus());

        verify(auditLogService).log(eq(mockUser), eq(AuditAction.APPLICATION_SUBMITTED),
                eq("APPLICATION"), eq(50L), contains("submitted"));
    }

    @Test
    void submit_forbiddenWhenNotOwner() {
        LoanApplication app = LoanApplication.builder()
                .applicationId(50L)
                .status(ApplicationStatus.DRAFT)
                .createdBy(AppUser.builder().userId(2L).build())
                .build();

        when(appRepo.findById(50L)).thenReturn(Optional.of(app));

        assertThrows(ApiException.class, () -> service.submit(50L));
    }
    @Test
    void submit_invalidStatus() {
        LoanApplication app = LoanApplication.builder()
                .applicationId(50L)
                .status(ApplicationStatus.UW_APPROVED)
                .createdBy(mockUser)
                .build();

        when(appRepo.findById(50L)).thenReturn(Optional.of(app));

        assertThrows(ApiException.class, () -> service.submit(50L));
    }
}
