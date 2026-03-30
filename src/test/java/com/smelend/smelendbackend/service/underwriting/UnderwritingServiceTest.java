package com.smelend.smelendbackend.service.underwriting;

import com.smelend.smelendbackend.dto.underwriting.UwDecisionRequest;
import com.smelend.smelendbackend.dto.underwriting.UwReviewResponse;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.*;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.repository.UwReviewRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class UnderwritingServiceTest {

    @Autowired
    private UnderwritingService underwritingService;

    @MockBean
    private LoanApplicationRepository appRepo;

    @MockBean
    private UwReviewRepository uwRepo;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private AuditLogService auditLogService;

    /**
     * ✅ queue()
     */
    @Test
    void queue_shouldReturnApplicationsRoutedToUW() {
        LoanApplication application = LoanApplication.builder()
                .applicationId(1L)
                .status(ApplicationStatus.ROUTED_TO_UW)
                .build();

        when(appRepo.findByStatus(ApplicationStatus.ROUTED_TO_UW))
                .thenReturn(List.of(application));

        var result = underwritingService.queue();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getApplicationId());
        verify(appRepo).findByStatus(ApplicationStatus.ROUTED_TO_UW);
    }

    /**
     * ✅ decide() - APPROVE scenario
     */
    @Test
    void decide_shouldApproveApplication() {
        // ---- Current user (UNDERWRITER) ----
        AppUser underwriter = AppUser.builder()
                .userId(100L)
                .email("uw@test.com")
                .role(Role.builder()
                        .roleName(RoleName.UNDERWRITER)
                        .build())
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(underwriter);

        // ---- Application ----
        LoanApplication application = LoanApplication.builder()
                .applicationId(10L)
                .status(ApplicationStatus.ROUTED_TO_UW)
                .build();

        when(appRepo.findById(10L)).thenReturn(Optional.of(application));
        when(appRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // ---- UW Review ----
        when(uwRepo.findByApplication_ApplicationId(10L))
                .thenReturn(Optional.empty());

        UwReview savedReview = UwReview.builder()
                .reviewId(50L)
                .decision(UwDecision.APPROVE)
                .summaryNote("Approved")
                .build();

        when(uwRepo.save(any())).thenReturn(savedReview);

        // ---- Request ----
        UwDecisionRequest request = new UwDecisionRequest();
        request.setDecision(UwDecision.APPROVE);
        request.setSummaryNote("Approved");

        // ---- Execute ----
        UwReviewResponse response = underwritingService.decide(10L, request);

        // ---- Assertions ----
        assertEquals(UwDecision.APPROVE, response.getDecision());
        assertEquals(ApplicationStatus.UW_APPROVED, response.getNewApplicationStatus());
        assertEquals(10L, response.getApplicationId());

        // ---- Verify side effects ----
        verify(appRepo).save(application);
        verify(uwRepo).save(any(UwReview.class));
        verify(auditLogService).log(
                eq(underwriter),
                eq(AuditAction.UW_DECISION),
                eq("APPLICATION"),
                eq(10L),
                contains("APPROVE")
        );
    }
}