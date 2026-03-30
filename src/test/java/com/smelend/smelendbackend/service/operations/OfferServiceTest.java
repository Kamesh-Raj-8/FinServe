package com.smelend.smelendbackend.service.operations;

import com.smelend.smelendbackend.dto.operations.offer.CreateOfferRequest;
import com.smelend.smelendbackend.dto.operations.offer.OfferResponse;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.*;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.repository.OfferRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock private OfferRepository offerRepo;
    @Mock private LoanApplicationRepository appRepo;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private OfferService offerService;

    private AppUser adminUser;
    private LoanApplication application;

    @BeforeEach
    void setUp() {
        // Fix: Use No-Args and Setters to avoid constructor mismatch
        Role adminRole = new Role();
        adminRole.setRoleName(RoleName.ADMIN);

        adminUser = new AppUser();
        adminUser.setUserId(1L);
        adminUser.setEmail("admin@smelend.com");
        adminUser.setRole(adminRole);

        application = new LoanApplication();
        application.setApplicationId(100L);
        application.setStatus(ApplicationStatus.UW_APPROVED);
    }

    @Test
    void createOffer_Success() {
        CreateOfferRequest req = new CreateOfferRequest();
        req.setSanctionedAmount(new BigDecimal("50000"));
        req.setInterestRate(new BigDecimal("10.5"));
        req.setEmiAmount(new BigDecimal("4500"));
        req.setValidUntil(LocalDate.now().plusDays(7));

        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(appRepo.findById(100L)).thenReturn(Optional.of(application));
        when(offerRepo.findByApplication_ApplicationId(100L)).thenReturn(Optional.empty());
        when(offerRepo.save(any(Offer.class))).thenAnswer(i -> {
            Offer o = i.getArgument(0);
            o.setOfferId(1L);
            return o;
        });

        OfferResponse response = offerService.createOffer(100L, req);

        assertNotNull(response);
        assertEquals(ApplicationStatus.OFFERED, application.getStatus());
        verify(auditLogService).log(any(), eq(AuditAction.OFFER_CREATED), any(), any(), any());
    }

    @Test
    void acceptOffer_Success() {
        Offer offer = new Offer();
        offer.setOfferId(1L);
        offer.setOfferStatus(OfferStatus.OFFERED);
        offer.setValidUntil(LocalDate.now().plusDays(1));
        offer.setApplication(application);

        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        when(currentUserService.isAdmin(any())).thenReturn(true);

        OfferResponse response = offerService.acceptOffer(1L);

        assertEquals(OfferStatus.ACCEPTED, offer.getOfferStatus());
        assertEquals(ApplicationStatus.OFFER_ACCEPTED, application.getStatus());
    }

    @Test
    void rejectOffer_Success() {
        Offer offer = new Offer();
        offer.setOfferId(1L);
        offer.setOfferStatus(OfferStatus.OFFERED);
        offer.setApplication(application);

        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        when(currentUserService.isAdmin(any())).thenReturn(true);

        OfferResponse response = offerService.rejectOffer(1L);

        assertEquals(OfferStatus.REJECTED, offer.getOfferStatus());
        assertEquals(ApplicationStatus.OFFER_REJECTED, application.getStatus());
    }

    @Test
    void get_Success() {
        Offer offer = new Offer();
        offer.setOfferId(1L);
        offer.setApplication(application);

        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));

        OfferResponse response = offerService.get(1L);

        assertNotNull(response);
        assertEquals(1L, response.getOfferId());
    }
}