package com.smelend.smelendbackend.dto.charge;
import com.smelend.smelendbackend.entity.enums.ChargeType;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class ChargeRequest {
    @NotNull private Long loanAccountId;
    @NotNull private ChargeType chargeType;
    @NotNull @Positive private BigDecimal amount;
    private String description;
    private LocalDate chargeDate;
}
