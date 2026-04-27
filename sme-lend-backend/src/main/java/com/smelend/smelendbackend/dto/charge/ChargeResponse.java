package com.smelend.smelendbackend.dto.charge;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChargeResponse {
    private Long chargeId;
    private Long loanAccountId;
    private String chargeType;
    private BigDecimal amount;
    private String description;
    private LocalDate chargeDate;
    private String status;
    private LocalDateTime createdAt;
}
