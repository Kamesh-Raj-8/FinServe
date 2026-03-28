package com.smelend.smelendbackend.service.collections;

import com.smelend.smelendbackend.entity.Delinquency;
import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.entity.RepaymentSchedule;
import com.smelend.smelendbackend.entity.enums.BucketType;
import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.DelinquencyRepository;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import com.smelend.smelendbackend.repository.RepaymentScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DpdServiceTest {

    @Mock
    private LoanAccountRepository loanRepo;
    @Mock
    private RepaymentScheduleRepository scheduleRepo;
    @Mock
    private DelinquencyRepository delinRepo;

    @InjectMocks
    private DpdService dpdService;

    private LoanAccount mockLoan;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockLoan = LoanAccount.builder()
                .loanAccountId(100L)
                .accountNumber("ACC123")
                .build();
    }

    @Test
    void computeAndUpsert_noOverdueInstallments_bucketCurrent() {
        RepaymentSchedule s1 = RepaymentSchedule.builder()
                .installmentNo(1)
                .loanAccount(mockLoan)
                .dueDate(LocalDate.now().plusDays(10))
                .status(InstallmentStatus.DUE)
                .build();

        when(loanRepo.findById(100L)).thenReturn(Optional.of(mockLoan));
        when(scheduleRepo.findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(100L))
                .thenReturn(List.of(s1));
        when(delinRepo.findByLoanAccount_LoanAccountId(100L)).thenReturn(Optional.empty());
        when(delinRepo.save(any(Delinquency.class))).thenAnswer(inv -> inv.getArgument(0));

        Delinquency result = dpdService.computeAndUpsert(100L);

        assertEquals(0, result.getDpd());
        assertEquals(BucketType.CURRENT, result.getBucket());
    }

    @Test
    void computeAndUpsert_singleOverdue_assignsCorrectBucket() {
        RepaymentSchedule s1 = RepaymentSchedule.builder()
                .installmentNo(1)
                .loanAccount(mockLoan)
                .dueDate(LocalDate.now().minusDays(40))
                .status(InstallmentStatus.DUE)
                .build();

        when(loanRepo.findById(100L)).thenReturn(Optional.of(mockLoan));
        when(scheduleRepo.findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(100L))
                .thenReturn(List.of(s1));
        when(delinRepo.findByLoanAccount_LoanAccountId(100L)).thenReturn(Optional.empty());
        when(delinRepo.save(any(Delinquency.class))).thenAnswer(inv -> inv.getArgument(0));

        Delinquency result = dpdService.computeAndUpsert(100L);

        assertTrue(result.getDpd() >= 40);
        assertEquals(BucketType.DPD_31_60, result.getBucket());
    }

    @Test
    void computeAndUpsert_multipleOverdue_takesMaxDpd() {
        RepaymentSchedule s1 = RepaymentSchedule.builder()
                .installmentNo(1)
                .loanAccount(mockLoan)
                .dueDate(LocalDate.now().minusDays(10))
                .status(InstallmentStatus.DUE)
                .build();

        RepaymentSchedule s2 = RepaymentSchedule.builder()
                .installmentNo(2)
                .loanAccount(mockLoan)
                .dueDate(LocalDate.now().minusDays(70))
                .status(InstallmentStatus.DUE)
                .build();

        when(loanRepo.findById(100L)).thenReturn(Optional.of(mockLoan));
        when(scheduleRepo.findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(100L))
                .thenReturn(List.of(s1, s2));
        when(delinRepo.findByLoanAccount_LoanAccountId(100L)).thenReturn(Optional.empty());
        when(delinRepo.save(any(Delinquency.class))).thenAnswer(inv -> inv.getArgument(0));

        Delinquency result = dpdService.computeAndUpsert(100L);

        assertTrue(result.getDpd() >= 70);
        assertEquals(BucketType.DPD_61_90, result.getBucket());
    }

    @Test
    void computeAndUpsert_updatesExistingDelinquency() {
        RepaymentSchedule s1 = RepaymentSchedule.builder()
                .installmentNo(1)
                .loanAccount(mockLoan)
                .dueDate(LocalDate.now().minusDays(5))
                .status(InstallmentStatus.DUE)
                .build();

        Delinquency existing = Delinquency.builder()
                .delinquencyId(1L)
                .loanAccount(mockLoan)
                .dpd(0)
                .bucket(BucketType.CURRENT)
                .build();

        when(loanRepo.findById(100L)).thenReturn(Optional.of(mockLoan));
        when(scheduleRepo.findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(100L))
                .thenReturn(List.of(s1));
        when(delinRepo.findByLoanAccount_LoanAccountId(100L)).thenReturn(Optional.of(existing));
        when(delinRepo.save(any(Delinquency.class))).thenAnswer(inv -> inv.getArgument(0));

        Delinquency result = dpdService.computeAndUpsert(100L);

        assertTrue(result.getDpd() >= 5);
        assertEquals(BucketType.DPD_1_30, result.getBucket());
    }

    @Test
    void computeAndUpsert_loanNotFoundThrowsException() {
        when(loanRepo.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> dpdService.computeAndUpsert(999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
