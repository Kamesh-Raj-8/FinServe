package com.smelend.smelendbackend.service.onboarding;

import com.smelend.smelendbackend.dto.onboarding.promoter.AddPromoterRequest;
import com.smelend.smelendbackend.dto.onboarding.promoter.PromoterResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.Promoter;
import com.smelend.smelendbackend.entity.Sme;
import com.smelend.smelendbackend.entity.enums.KycStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.PromoterRepository;
import com.smelend.smelendbackend.repository.SmeRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromoterServiceTest {

    private PromoterRepository promoterRepo;
    private SmeRepository smeRepo;
    private CurrentUserService currentUserService;
    private PromoterService service;

    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        promoterRepo = mock(PromoterRepository.class);
        smeRepo = mock(SmeRepository.class);
        currentUserService = mock(CurrentUserService.class);

        service = new PromoterService(promoterRepo, smeRepo, currentUserService);

        mockUser = AppUser.builder().userId(1L).email("user@test.com").build();
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    void addPromoter_success() {
        Sme sme = Sme.builder().smeId(10L).createdBy(mockUser).build();
        AddPromoterRequest req = AddPromoterRequest.builder()
                .promoterName("John Doe")
                .mobile("9999999999")
                .ownershipPct(BigDecimal.valueOf(50.0))
                .build();

        when(smeRepo.findById(10L)).thenReturn(Optional.of(sme));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);
        when(promoterRepo.save(any())).thenAnswer(invocation -> {
            Promoter p = invocation.getArgument(0);
            p.setPromoterId(100L);
            return p;
        });

        PromoterResponse response = service.addPromoter(10L, req);

        assertNotNull(response);
        assertEquals(100L, response.getPromoterId());
        assertEquals("John Doe", response.getPromoterName());
        assertEquals(KycStatus.PENDING, response.getKycStatus());
        assertEquals(BigDecimal.valueOf(50.0), response.getOwnershipPct());
    }

    @Test
    void addPromoter_forbiddenWhenNotOwnerOrAdmin() {
        Sme sme = Sme.builder().smeId(10L).createdBy(AppUser.builder().userId(2L).build()).build();
        AddPromoterRequest req = AddPromoterRequest.builder()
                .promoterName("Jane")
                .mobile("8888888888")
                .ownershipPct(BigDecimal.valueOf(40.0))
                .build();

        when(smeRepo.findById(10L)).thenReturn(Optional.of(sme));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);

        assertThrows(ApiException.class, () -> service.addPromoter(10L, req));
    }

    @Test
    void listBySme_success() {
        Sme sme = Sme.builder().smeId(10L).createdBy(mockUser).build();
        Promoter promoter = Promoter.builder()
                .promoterId(100L)
                .sme(sme)
                .promoterName("Alice")
                .mobile("7777777777")
                .ownershipPct(BigDecimal.valueOf(60.0))
                .kycStatus(KycStatus.PENDING)
                .createdBy(mockUser)
                .build();

        when(smeRepo.findById(10L)).thenReturn(Optional.of(sme));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);
        when(promoterRepo.findBySme_SmeId(10L)).thenReturn(List.of(promoter));

        List<PromoterResponse> responses = service.listBySme(10L);

        assertEquals(1, responses.size());
        assertEquals("Alice", responses.get(0).getPromoterName());
        assertEquals(BigDecimal.valueOf(60.0), responses.get(0).getOwnershipPct());
    }

    @Test
    void listBySme_forbiddenWhenNotOwnerOrAdmin() {
        Sme sme = Sme.builder().smeId(10L).createdBy(AppUser.builder().userId(2L).build()).build();

        when(smeRepo.findById(10L)).thenReturn(Optional.of(sme));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);

        assertThrows(ApiException.class, () -> service.listBySme(10L));
    }

    @Test
    void get_success() {
        Sme sme = Sme.builder().smeId(10L).createdBy(mockUser).build();
        Promoter promoter = Promoter.builder()
                .promoterId(100L)
                .sme(sme)
                .promoterName("Bob")
                .mobile("6666666666")
                .ownershipPct(BigDecimal.valueOf(30.0))
                .kycStatus(KycStatus.PENDING)
                .createdBy(mockUser)
                .build();

        when(promoterRepo.findById(100L)).thenReturn(Optional.of(promoter));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);

        PromoterResponse response = service.get(100L);

        assertEquals("Bob", response.getPromoterName());
        assertEquals(BigDecimal.valueOf(30.0), response.getOwnershipPct());
    }

    @Test
    void get_forbiddenWhenNotOwnerOrAdmin() {
        Sme sme = Sme.builder().smeId(10L).createdBy(AppUser.builder().userId(2L).build()).build();
        Promoter promoter = Promoter.builder()
                .promoterId(100L)
                .sme(sme)
                .promoterName("Charlie")
                .ownershipPct(BigDecimal.valueOf(20.0))
                .build();

        when(promoterRepo.findById(100L)).thenReturn(Optional.of(promoter));
        when(currentUserService.isAdmin(mockUser)).thenReturn(false);

        assertThrows(ApiException.class, () -> service.get(100L));
    }
}
