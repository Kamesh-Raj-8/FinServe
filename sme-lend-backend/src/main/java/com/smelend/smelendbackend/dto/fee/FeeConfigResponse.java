package com.smelend.smelendbackend.dto.fee;
import com.smelend.smelendbackend.entity.enums.FeeMode;
import com.smelend.smelendbackend.entity.enums.FeeType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeeConfigResponse {
    private Long feeId;
    private Long productId;
    private String productName;
    private FeeType feeType;
    private FeeMode feeMode;
    private BigDecimal value;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status;
}
