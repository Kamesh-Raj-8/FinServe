package com.smelend.smelendbackend.service.risk;

import com.smelend.smelendbackend.dto.risk.PortfolioMetricsResponse;
import com.smelend.smelendbackend.entity.Delinquency;
import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.entity.enums.BucketType;
import com.smelend.smelendbackend.entity.enums.LoanAccountStatus;
import com.smelend.smelendbackend.repository.DelinquencyRepository;
import com.smelend.smelendbackend.repository.LoanAccountRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class RiskService {

    private final LoanAccountRepository loanRepo;
    private final DelinquencyRepository delinRepo;

    public RiskService(LoanAccountRepository loanRepo, DelinquencyRepository delinRepo) {
        this.loanRepo = loanRepo;
        this.delinRepo = delinRepo;
    }

    public PortfolioMetricsResponse portfolioMetrics() {

        List<LoanAccount> loans = loanRepo.findAll();

        long total = loans.size();
        long active = loans.stream().filter(l -> l.getStatus() == LoanAccountStatus.ACTIVE).count();

        List<Delinquency> dels = delinRepo.findAll();

        long delinquent = dels.stream().filter(d -> d.getDpd() != null && d.getDpd() > 0).count();

        Map<BucketType, Long> bucketCounts = new EnumMap<>(BucketType.class);
        for (BucketType b : BucketType.values()) bucketCounts.put(b, 0L);

        for (Delinquency d : dels) {
            BucketType b = d.getBucket() != null ? d.getBucket() : BucketType.CURRENT;
            bucketCounts.put(b, bucketCounts.getOrDefault(b, 0L) + 1);
        }

        return PortfolioMetricsResponse.builder()
                .totalLoanAccounts(total)
                .activeLoanAccounts(active)
                .delinquentLoanAccounts(delinquent)
                .bucketCounts(bucketCounts)
                .build();
    }
}