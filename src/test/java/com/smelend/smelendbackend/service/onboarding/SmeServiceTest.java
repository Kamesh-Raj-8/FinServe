package com.smelend.smelendbackend.service.onboarding;

import com.smelend.smelendbackend.dto.onboarding.sme.CreateSmeRequest;
import com.smelend.smelendbackend.dto.onboarding.sme.SmeResponse;
import com.smelend.smelendbackend.dto.onboarding.sme.UpdateSmeRequest;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.Sme;
import com.smelend.smelendbackend.entity.enums.AuditAction;
import com.smelend.smelendbackend.entity.enums.BusinessType;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.SmeRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmeServiceTest {

    private SmeRepository smeRepo;
    private CurrentUserService currentUserService;
    private AuditLogService auditLogService;
    private SmeService service;

    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        smeRepo = mock(SmeRepository.class);
        currentUserService = mock(CurrentUserService.class);
        auditLogService = mock(AuditLogService.class);

        service = new SmeService(smeRepo, currentUserService, auditLogService);

        mockUser = AppUser.builder().userId(1L).email("user@test.com").build();
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    void create_success() {
        CreateSmeRequest req = CreateSmeRequest.builder()
                .legalName("Legal SME")
                .tradeName("Trade SME")
                .businessType(BusinessType.PROPRIETORSHIP)
                .industry("Textiles")
                .address("123 Street")
                .gstNo("GST123")
                .build();

        when(smeRepo.save(any())).thenAnswer(invocation -> {
            Sme sme = invocation.getArgument(0);
            sme.setSmeId(100L);
            return sme;
        });

        SmeResponse response = service.create(req);

        assertNotNull(response);
        assertEquals(100L, response.getSmeId());
        assertEquals("Legal SME", response.getLegalName());
        assertEquals(StatusFlag.ACTIVE, response.getStatus());
        assertEquals(BusinessType.PROPRIETORSHIP, response.getBusinessType());

        verify(auditLogService).log(eq(mockUser), eq(AuditAction.SME_CREATED),
                eq("SME"), eq(100L), contains("SME created"));
    }

    @Test
    void update_success() {
        Sme sme = Sme.builder()
                .smeId(100L)
                .legalName("Old Name")
                .createdBy(mockUser)
                .build();

        UpdateSmeRequest req = UpdateSmeRequest.builder()
                .legalName("New Name")
                .tradeName("New Trade")
                .businessType(BusinessType.PVT_LTD)
                .industry("Automotive")
                .address("456 Avenue")
                .gstNo("GST456")
                .build();

        when(smeRepo.findById(100L)).thenReturn(Optional.of(sme));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);
        when(smeRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SmeResponse response = service.update(100L, req);

        assertEquals("New Name", response.getLegalName());
        assertEquals("New Trade", response.getTradeName());
        assertEquals(BusinessType.PVT_LTD, response.getBusinessType());

        verify(auditLogService).log(eq(mockUser), eq(AuditAction.SME_UPDATED),
                eq("SME"), eq(100L), contains("SME updated"));
    }

    @Test
    void update_forbiddenWhenNotOwnerOrAdmin() {
        Sme sme = Sme.builder()
                .smeId(100L)
                .createdBy(AppUser.builder().userId(2L).build()) // different owner
                .build();

        UpdateSmeRequest req = UpdateSmeRequest.builder()
                .legalName("New Name")
                .businessType(BusinessType.LLP)
                .build();

        when(smeRepo.findById(100L)).thenReturn(Optional.of(sme));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);

        assertThrows(ApiException.class, () -> service.update(100L, req));
    }

    @Test
    void get_success() {
        Sme sme = Sme.builder()
                .smeId(100L)
                .legalName("Legal SME")
                .businessType(BusinessType.PARTNERSHIP)
                .createdBy(mockUser)
                .build();

        when(smeRepo.findById(100L)).thenReturn(Optional.of(sme));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);

        SmeResponse response = service.get(100L);

        assertEquals("Legal SME", response.getLegalName());
        assertEquals(100L, response.getSmeId());
        assertEquals(BusinessType.PARTNERSHIP, response.getBusinessType());
    }

    @Test
    void get_forbiddenWhenNotOwnerOrAdmin() {
        Sme sme = Sme.builder()
                .smeId(100L)
                .createdBy(AppUser.builder().userId(2L).build())
                .businessType(BusinessType.OTHER)
                .build();

        when(smeRepo.findById(100L)).thenReturn(Optional.of(sme));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);

        assertThrows(ApiException.class, () -> service.get(100L));
    }

    @Test
    void listMine_success() {
        Sme sme = Sme.builder()
                .smeId(100L)
                .legalName("Legal SME")
                .businessType(BusinessType.LLP)
                .createdBy(mockUser)
                .build();

        when(smeRepo.findByCreatedBy_UserId(1L)).thenReturn(List.of(sme));

        List<SmeResponse> responses = service.listMine();

        assertEquals(1, responses.size());
        assertEquals("Legal SME", responses.get(0).getLegalName());
        assertEquals(BusinessType.LLP, responses.get(0).getBusinessType());
    }
}
