package com.smelend.smelendbackend.service.collections;

import com.smelend.smelendbackend.dto.collections.CreatePtpRequest;
import com.smelend.smelendbackend.dto.collections.DelinquencyResponse;
import com.smelend.smelendbackend.dto.collections.PtpResponse;
import com.smelend.smelendbackend.entity.*;
import com.smelend.smelendbackend.entity.enums.PtpStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.DelinquencyRepository;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import com.smelend.smelendbackend.repository.PtpRepository;
import com.smelend.smelendbackend.service.common.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollectionsServiceTest {

    @Mock
    private LoanAccountRepository loanRepo;
    @Mock
    private DelinquencyRepository delinRepo;
    @Mock
    private PtpRepository ptpRepo;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private DpdService dpdService;

    @InjectMocks
    private CollectionsService collectionsService;

    private AppUser mockUser;
    private LoanAccount mockLoan;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockUser = AppUser.builder()
                .userId(1L)
                .email("test@example.com")
                .role(Role.builder().roleName(com.smelend.smelendbackend.entity.enums.RoleName.COLLECTIONS).build())
                .build();

        mockLoan = LoanAccount.builder()
                .loanAccountId(100L)
                .accountNumber("ACC123")
                .build();
    }

    @Test
    void getDelinquency_success() {
        Delinquency delinquency = Delinquency.builder()
                .delinquencyId(1L)
                .loanAccount(mockLoan)
                .dpd(15)
                .bucket(com.smelend.smelendbackend.entity.enums.BucketType.DPD_1_30)
                .asOfDate(LocalDate.now())
                .updatedDate(LocalDateTime.now())
                .build();

        when(loanRepo.findById(100L)).thenReturn(Optional.of(mockLoan));
        when(dpdService.computeAndUpsert(100L)).thenReturn(delinquency);

        DelinquencyResponse response = collectionsService.getDelinquency(100L);

        assertEquals(1L, response.getDelinquencyId());
        assertEquals(100L, response.getLoanAccountId());
        assertEquals(15, response.getDpd());
    }

    @Test
    void getDelinquency_notFound() {
        when(loanRepo.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> collectionsService.getDelinquency(999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void listAllDelinquencies_success() {
        Delinquency d1 = Delinquency.builder().delinquencyId(1L).loanAccount(mockLoan).dpd(5).bucket(com.smelend.smelendbackend.entity.enums.BucketType.DPD_1_30).build();
        Delinquency d2 = Delinquency.builder().delinquencyId(2L).loanAccount(mockLoan).dpd(40).bucket(com.smelend.smelendbackend.entity.enums.BucketType.DPD_31_60).build();

        when(delinRepo.findAll()).thenReturn(List.of(d1, d2));

        List<DelinquencyResponse> responses = collectionsService.listAllDelinquencies();

        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).getDelinquencyId());
        assertEquals(2L, responses.get(1).getDelinquencyId());
    }

    @Test
    void createPtp_success() {
        CreatePtpRequest req = new CreatePtpRequest();
        req.setLoanAccountId(100L);
        req.setPromiseDate(LocalDate.now().plusDays(7));
        req.setPromisedAmount(BigDecimal.valueOf(5000));
        req.setNotes("Promise to pay");

        when(currentUserService.getCurrentUser()).thenReturn(mockUser);
        when(loanRepo.findById(100L)).thenReturn(Optional.of(mockLoan));
        when(ptpRepo.save(any(Ptp.class))).thenAnswer(inv -> {
            Ptp p = inv.getArgument(0);
            p.setPtpId(10L);
            return p;
        });

        PtpResponse response = collectionsService.createPtp(req);

        assertEquals(10L, response.getPtpId());
        assertEquals(100L, response.getLoanAccountId());
        assertEquals(PtpStatus.OPEN, response.getStatus());
        assertEquals("Promise to pay", response.getNotes());
        assertEquals(mockUser.getEmail(), response.getCreatedByEmail());
    }

    @Test
    void listPtps_success() {
        Ptp ptp1 = Ptp.builder().ptpId(1L).loanAccount(mockLoan).promiseDate(LocalDate.now()).promisedAmount(BigDecimal.valueOf(1000)).status(PtpStatus.OPEN).createdBy(mockUser).createdDate(LocalDateTime.now()).build();
        Ptp ptp2 = Ptp.builder().ptpId(2L).loanAccount(mockLoan).promiseDate(LocalDate.now().plusDays(1)).promisedAmount(BigDecimal.valueOf(2000)).status(PtpStatus.OPEN).createdBy(mockUser).createdDate(LocalDateTime.now()).build();

        when(loanRepo.findById(100L)).thenReturn(Optional.of(mockLoan));
        when(ptpRepo.findByLoanAccount_LoanAccountIdOrderByCreatedDateDesc(100L)).thenReturn(List.of(ptp1, ptp2));

        List<PtpResponse> responses = collectionsService.listPtps(100L);

        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).getPtpId());
        assertEquals(2L, responses.get(1).getPtpId());
    }

    @Test
    void updatePtpStatus_success() {
        Ptp ptp = Ptp.builder()
                .ptpId(5L)
                .loanAccount(mockLoan)
                .status(PtpStatus.OPEN)
                .createdBy(mockUser)
                .createdDate(LocalDateTime.now())
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(mockUser);
        when(ptpRepo.findById(5L)).thenReturn(Optional.of(ptp));
        when(ptpRepo.save(any(Ptp.class))).thenAnswer(inv -> inv.getArgument(0));

        PtpResponse response = collectionsService.updatePtpStatus(5L, PtpStatus.CANCELLED);

        assertEquals(PtpStatus.CANCELLED, response.getStatus());
    }

    @Test
    void updatePtpStatus_notFound() {
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);
        when(ptpRepo.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> collectionsService.updatePtpStatus(999L, PtpStatus.CANCELLED));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
