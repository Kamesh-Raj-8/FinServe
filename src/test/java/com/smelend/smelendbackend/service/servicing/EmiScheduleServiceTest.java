package com.smelend.smelendbackend.service.servicing;

import com.smelend.smelendbackend.dto.servicing.schedule.ScheduleResponse;
import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.entity.RepaymentSchedule;
import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import com.smelend.smelendbackend.repository.RepaymentScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmiScheduleServiceTest {

    @Mock
    private LoanAccountRepository loanRepo;

    @Mock
    private RepaymentScheduleRepository scheduleRepo;

    @InjectMocks
    private EmiScheduleService service;

    private LoanAccount loan;

    @BeforeEach
    void setup() {
        loan = LoanAccount.builder()
                .loanAccountId(1L)
                .principalSanctioned(new BigDecimal("120000"))
                .tenorMonths(12)
                .interestRate(new BigDecimal("12")) // 12% annual
                .startDate(LocalDate.of(2024, 1, 1))
                .build();
    }

    // ---------------------------
    // generateIfNotExists
    // ---------------------------

    @Test
    void generateIfNotExists_loanNotFound() {
        when(loanRepo.findById(1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.generateIfNotExists(1L));

        assertEquals("Loan account not found", ex.getMessage());
    }

    @Test
    void generateIfNotExists_existingScheduleReturned() {
        RepaymentSchedule schedule = RepaymentSchedule.builder()
                .scheduleId(10L)
                .loanAccount(loan)
                .installmentNo(1)
                .principalDue(new BigDecimal("10000"))
                .interestDue(new BigDecimal("1200"))
                .totalDue(new BigDecimal("11200"))
                .amountPaid(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("11200"))
                .status(InstallmentStatus.DUE)
                .build();

        when(loanRepo.findById(1L)).thenReturn(Optional.of(loan));
        when(scheduleRepo
                .findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(1L))
                .thenReturn(List.of(schedule));

        List<ScheduleResponse> result = service.generateIfNotExists(1L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getInstallmentNo());
        verify(scheduleRepo, never()).saveAll(any());
    }

    @Test
    void generateIfNotExists_generateScheduleSuccessfully() {
        when(loanRepo.findById(1L)).thenReturn(Optional.of(loan));
        when(scheduleRepo
                .findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(1L))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<List<RepaymentSchedule>> captor =
                ArgumentCaptor.forClass(List.class);

        List<ScheduleResponse> result = service.generateIfNotExists(1L);

        verify(scheduleRepo).saveAll(captor.capture());

        List<RepaymentSchedule> saved = captor.getValue();

        assertEquals(12, saved.size());

        RepaymentSchedule first = saved.get(0);
        assertEquals(1, first.getInstallmentNo());
        assertEquals(LocalDate.of(2024, 2, 1), first.getDueDate());
        assertEquals(new BigDecimal("10000.00"), first.getPrincipalDue());
        assertEquals(InstallmentStatus.DUE, first.getStatus());

        assertEquals(12, result.size());
    }

    // ---------------------------
    // list
    // ---------------------------

    @Test
    void list_loanNotFound() {
        when(loanRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> service.list(1L));
    }

    @Test
    void list_returnsSchedule() {
        RepaymentSchedule schedule = RepaymentSchedule.builder()
                .scheduleId(11L)
                .loanAccount(loan)
                .installmentNo(1)
                .principalDue(new BigDecimal("10000"))
                .interestDue(new BigDecimal("1200"))
                .totalDue(new BigDecimal("11200"))
                .amountPaid(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("11200"))
                .status(InstallmentStatus.DUE)
                .build();

        when(loanRepo.findById(1L)).thenReturn(Optional.of(loan));
        when(scheduleRepo
                .findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(1L))
                .thenReturn(List.of(schedule));

        List<ScheduleResponse> result = service.list(1L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getInstallmentNo());
    }
}