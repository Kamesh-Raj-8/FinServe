package com.smelend.smelendbackend.dto.fee;
import lombok.*;
import java.math.BigDecimal;

/** A single fee calculated and applied at disbursement. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppliedFeeDto {
    private String feeType;
    private String feeMode;
    private BigDecimal configuredValue;
    private BigDecimal calculatedAmount;
}
