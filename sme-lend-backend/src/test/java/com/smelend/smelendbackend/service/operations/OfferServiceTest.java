package com.smelend.smelendbackend.service.operations;

import com.smelend.smelendbackend.dto.operations.offer.CreateOfferRequest;
import com.smelend.smelendbackend.dto.operations.offer.OfferResponse;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.*;
import com.smelend.smelendbackend.repository.LoanApplicationRepository;
import com.smelend.smelendbackend.repository.OfferRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import com.smelend.smelendbackend.service.compliance.AuditLogService;
import com.smelend.smelendbackend.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
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
    @Mock private NotificationService notificationService;

    @InjectMocks
    private OfferService offerService;

    private AppUser opsUser;
    private LoanApplication application;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRoleName(RoleName.OPERATIONS);
        opsUser = new AppUser();
        opsUser.setUserId(1L);
        opsUser.setRole(role);
        application = new LoanApplication();
        application.setApplicationId(101L);
        application.setStatus(ApplicationStatus.UW_APPROVED);
        AppUser creator = new AppUser();
        creator.setUserId(1L);
        application.setCreatedBy(creator);
    }

    @Test
    void createOffer() {
        CreateOfferRequest req = new CreateOfferRequest();
        req.setSanctionedAmount(new BigDecimal("5000.00"));
        req.setInterestRate(new BigDecimal("10.0"));
        req.setEmiAmount(new BigDecimal("500.00"));
        req.setValidUntil(LocalDate.now().plusDays(7));
        when(currentUserService.getCurrentUser()).thenReturn(opsUser);
        when(appRepo.findById(101L)).thenReturn(Optional.of(application));
        when(offerRepo.findByApplication_ApplicationId(101L)).thenReturn(Optional.empty());
        when(offerRepo.save(any())).thenAnswer(i -> i.getArguments()[0]);
        OfferResponse resp = offerService.createOffer(101L, req);
        assertEquals(OfferStatus.OFFERED, resp.getOfferStatus());
        assertEquals(ApplicationStatus.OFFERED, application.getStatus());
        verify(offerRepo).save(any());
    }

    @Test
    void acceptOffer() {
        Offer offer = Offer.builder()
                .offerId(1L)
                .application(application)
                .offerStatus(OfferStatus.OFFERED)
                .validUntil(LocalDate.now().plusDays(5))
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(opsUser);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        lenient().when(currentUserService.isAdmin(opsUser)).thenReturn(true);
        OfferResponse resp = offerService.acceptOffer(1L);
        assertEquals(OfferStatus.ACCEPTED, resp.getOfferStatus());
        assertEquals(ApplicationStatus.OFFER_ACCEPTED, application.getStatus());
        verify(offerRepo).save(any(Offer.class));
    }

    @Test
    void rejectOffer() {
        Offer offer = Offer.builder()
                .offerId(1L)
                .application(application)
                .offerStatus(OfferStatus.OFFERED)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(opsUser);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        lenient().when(currentUserService.isAdmin(opsUser)).thenReturn(true);
        OfferResponse resp = offerService.rejectOffer(1L);
        assertEquals(OfferStatus.REJECTED, resp.getOfferStatus());
        assertEquals(ApplicationStatus.OFFER_REJECTED, application.getStatus());
        verify(offerRepo).save(any(Offer.class));
    }

    @Test
    void get() {
        Offer offer = Offer.builder().offerId(1L).application(application).build();
        when(currentUserService.getCurrentUser()).thenReturn(opsUser);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        OfferResponse resp = offerService.get(1L);
        assertNotNull(resp);
        assertEquals(1L, resp.getOfferId());
    }

    @Test
    void listMineOrAll() {
        when(currentUserService.getCurrentUser()).thenReturn(opsUser);
        when(offerRepo.findAll()).thenReturn(List.of(new Offer()));
        List<OfferResponse> list = offerService.listMineOrAll();
        assertFalse(list.isEmpty());
        verify(offerRepo).findAll();
    }

    @Test
    void createAutoOffer() {
        LoanProduct product = new LoanProduct();
        product.setBaseInterestRate(new BigDecimal("12.00"));
        application.setProduct(product);
        application.setRequestedAmount(new BigDecimal("120000.00"));
        application.setTenorMonths(12);
        when(offerRepo.findByApplication_ApplicationId(any())).thenReturn(Optional.empty());
        when(offerRepo.save(any())).thenAnswer(i -> i.getArguments()[0]);
        offerService.createAutoOffer(application);
        verify(offerRepo).save(argThat(o ->
                o.getEmiAmount().setScale(2, RoundingMode.HALF_UP).equals(new BigDecimal("10661.85"))
        ));
    }
}