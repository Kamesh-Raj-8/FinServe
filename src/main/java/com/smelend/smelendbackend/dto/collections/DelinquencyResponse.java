package com.smelend.smelendbackend.dto.collections;

import com.smelend.smelendbackend.entity.enums.BucketType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DelinquencyResponse {

    private Long delinquencyId;
    private Long loanAccountId;

    private Integer dpd;
    private BucketType bucket;

    private LocalDate asOfDate;
    private LocalDateTime updatedDate;
}