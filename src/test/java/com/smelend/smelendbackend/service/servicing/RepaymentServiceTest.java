package com.smelend.smelendbackend.service.servicing;

import com.smelend.smelendbackend.dto.servicing.repayment.PostRepaymentRequest;
import com.smelend.smelendbackend.dto.servicing.repayment.RepaymentResponse;
import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.entity.Repayment;
import com.smelend.smelendbackend.entity.RepaymentSchedule;
import com.smelend.smelendbackend.entity.enums.InstallmentStatus;
import com.smelend.smelendbackend.entity.enums.RepaymentMode;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import com.smelend.smelendbackend.repository.RepaymentRepository;
import com.smelend.smelendbackend.repository.RepaymentScheduleRepository;
import com.smelend.smelendbackend.service.collections.DpdService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTest {

    @Mock
    LoanAccountRepository loanRepo;

    @Mock
    RepaymentRepository repayRepo;

    @Mock
    RepaymentScheduleRepository scheduleRepo;

    @Mock
    DpdService dpdService;

    @InjectMocks
    RepaymentService service;

    LoanAccount loan;

    @BeforeEach
    void setup() {
        loan = LoanAccount.builder()
                .loanAccountId(1L)
                .build();
    }

    // ------------------------------------------------------
    // POST
    // ------------------------------------------------------
    @Nested
    class PostRepayment {

        PostRepaymentRequest request;

        @BeforeEach
        void setup() {
            request = PostRepaymentRequest.builder()
                    .loanAccountId(1L)
                    .amount(new BigDecimal("1500.00"))
                    .paymentDate(LocalDate.of(2024, 3, 10))
                    .mode(RepaymentMode.UPI)
                    .referenceNo("REF123")
                    .build();
        }

        @Test
        void throwsException_whenLoanNotFound() {
            when(loanRepo.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ApiException.class, () -> service.post(request));
        }

        @Test
        void postsRepayment_andAllocatesAcrossSchedules() {
            when(loanRepo.findById(1L)).thenReturn(Optional.of(loan));
            when(repayRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            RepaymentSchedule s1 = RepaymentSchedule.builder()
                    .installmentNo(1)
                    .totalDue(new BigDecimal("1000.00"))
                    .balanceDue(new BigDecimal("1000.00"))
                    .amountPaid(BigDecimal.ZERO)
                    .dueDate(LocalDate.of(2024, 3, 1))
                    .status(InstallmentStatus.DUE)
                    .build();

            RepaymentSchedule s2 = RepaymentSchedule.builder()
                    .installmentNo(2)
                    .totalDue(new BigDecimal("1000.00"))
                    .balanceDue(new BigDecimal("1000.00"))
                    .amountPaid(BigDecimal.ZERO)
                    .dueDate(LocalDate.of(2024, 4, 1))
                    .status(InstallmentStatus.DUE)
                    .build();

            when(scheduleRepo
                    .findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(1L))
                    .thenReturn(List.of(s1, s2));

            RepaymentResponse response = service.post(request);

            assertThat(response.getAmount()).isEqualByComparingTo("1500.00");

            // First installment fully paid
            assertThat(s1.getBalanceDue()).isEqualByComparingTo("0.00");
            assertThat(s1.getStatus()).isEqualTo(InstallmentStatus.PAID);

            // Second installment partially paid
            assertThat(s2.getBalanceDue()).isEqualByComparingTo("500.00");
            assertThat(s2.getStatus()).isEqualTo(InstallmentStatus.DUE);

            verify(scheduleRepo, times(2)).save(any(RepaymentSchedule.class));
            verify(dpdService).computeAndUpsert(1L);
        }

        @Test
        void marksScheduleOverdue_whenPaymentAfterDueDate() {
            when(loanRepo.findById(1L)).thenReturn(Optional.of(loan));
            when(repayRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .installmentNo(1)
                    .totalDue(new BigDecimal("1000.00"))
                    .balanceDue(new BigDecimal("1000.00"))
                    .amountPaid(BigDecimal.ZERO)
                    .dueDate(LocalDate.of(2024, 3, 1))
                    .build();

            when(scheduleRepo
                    .findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(1L))
                    .thenReturn(List.of(schedule));

            service.post(request);

            assertThat(schedule.getStatus())
                    .isEqualTo(InstallmentStatus.OVERDUE);
        }
    }

    // ------------------------------------------------------
    // LIST
    // ------------------------------------------------------
    @Nested
    class ListRepayments {

        @Test
        void throwsException_whenLoanNotFound() {
            when(loanRepo.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ApiException.class, () -> service.list(1L));
        }

        @Test
        void returnsRepaymentList() {
            when(loanRepo.findById(1L)).thenReturn(Optional.of(loan));

            Repayment r = Repayment.builder()
                    .repaymentId(101L)
                    .loanAccount(loan)
                    .amount(new BigDecimal("500"))
                    .paymentDate(LocalDate.now())
                    .build();

            when(repayRepo
                    .findByLoanAccount_LoanAccountIdOrderByPaymentDateDesc(1L))
                    .thenReturn(List.of(r));

            List<RepaymentResponse> responses = service.list(1L);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getAmount())
                    .isEqualByComparingTo("500");
        }
    }
}