package com.smelend.smelendbackend.dto.risk;

import com.smelend.smelendbackend.entity.enums.BucketType;
import lombok.*;

import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioMetricsResponse {

    private long totalLoanAccounts;
    private long activeLoanAccounts;

    private long delinquentLoanAccounts; // dpd > 0

    // Bucket counts
    private Map<BucketType, Long> bucketCounts;
}