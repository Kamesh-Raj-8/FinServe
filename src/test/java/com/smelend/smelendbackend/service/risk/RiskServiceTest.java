package com.smelend.smelendbackend.service.risk;

import com.smelend.smelendbackend.dto.risk.PortfolioMetricsResponse;
import com.smelend.smelendbackend.entity.Delinquency;
import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.entity.enums.BucketType;
import com.smelend.smelendbackend.entity.enums.LoanAccountStatus;
import com.smelend.smelendbackend.repository.DelinquencyRepository;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RiskServiceTest {

    @Mock
    private LoanAccountRepository loanRepo;
    @Mock
    private DelinquencyRepository delinRepo;

    @InjectMocks
    private RiskService riskService;

    private LoanAccount activeLoan;
    private LoanAccount closedLoan;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        activeLoan = LoanAccount.builder()
                .loanAccountId(1L)
                .status(LoanAccountStatus.ACTIVE)
                .build();

        closedLoan = LoanAccount.builder()
                .loanAccountId(2L)
                .status(LoanAccountStatus.CLOSED)
                .build();
    }

    @Test
    void portfolioMetrics_mixedLoansAndDelinquencies() {
        Delinquency d1 = Delinquency.builder()
                .delinquencyId(1L)
                .loanAccount(activeLoan)
                .dpd(10)
                .bucket(BucketType.DPD_1_30)
                .build();

        Delinquency d2 = Delinquency.builder()
                .delinquencyId(2L)
                .loanAccount(closedLoan)
                .dpd(0)
                .bucket(BucketType.CURRENT)
                .build();

        when(loanRepo.findAll()).thenReturn(List.of(activeLoan, closedLoan));
        when(delinRepo.findAll()).thenReturn(List.of(d1, d2));

        PortfolioMetricsResponse response = riskService.portfolioMetrics();

        assertEquals(2, response.getTotalLoanAccounts());
        assertEquals(1, response.getActiveLoanAccounts());
        assertEquals(1, response.getDelinquentLoanAccounts());
        assertEquals(1L, response.getBucketCounts().get(BucketType.DPD_1_30));
        assertEquals(1L, response.getBucketCounts().get(BucketType.CURRENT));
    }

    @Test
    void portfolioMetrics_noLoansOrDelinquencies() {
        when(loanRepo.findAll()).thenReturn(List.of());
        when(delinRepo.findAll()).thenReturn(List.of());

        PortfolioMetricsResponse response = riskService.portfolioMetrics();

        assertEquals(0, response.getTotalLoanAccounts());
        assertEquals(0, response.getActiveLoanAccounts());
        assertEquals(0, response.getDelinquentLoanAccounts());

        // All buckets initialized to 0
        for (BucketType b : BucketType.values()) {
            assertEquals(0L, response.getBucketCounts().get(b));
        }
    }

    @Test
    void portfolioMetrics_onlyActiveLoans() {
        when(loanRepo.findAll()).thenReturn(List.of(activeLoan));
        when(delinRepo.findAll()).thenReturn(List.of());

        PortfolioMetricsResponse response = riskService.portfolioMetrics();

        assertEquals(1, response.getTotalLoanAccounts());
        assertEquals(1, response.getActiveLoanAccounts());
        assertEquals(0, response.getDelinquentLoanAccounts());
    }

    @Test
    void portfolioMetrics_multipleBuckets() {
        Delinquency d1 = Delinquency.builder().delinquencyId(1L).loanAccount(activeLoan).dpd(5).bucket(BucketType.DPD_1_30).build();
        Delinquency d2 = Delinquency.builder().delinquencyId(2L).loanAccount(activeLoan).dpd(40).bucket(BucketType.DPD_31_60).build();
        Delinquency d3 = Delinquency.builder().delinquencyId(3L).loanAccount(activeLoan).dpd(95).bucket(BucketType.DPD_90_PLUS).build();

        when(loanRepo.findAll()).thenReturn(List.of(activeLoan));
        when(delinRepo.findAll()).thenReturn(List.of(d1, d2, d3));

        PortfolioMetricsResponse response = riskService.portfolioMetrics();

        Map<BucketType, Long> counts = response.getBucketCounts();
        assertEquals(1L, counts.get(BucketType.DPD_1_30));
        assertEquals(1L, counts.get(BucketType.DPD_31_60));
        assertEquals(1L, counts.get(BucketType.DPD_90_PLUS));
    }
}
