package com.smelend.smelendbackend.dto.fee;
import com.smelend.smelendbackend.entity.enums.FeeMode;
import com.smelend.smelendbackend.entity.enums.FeeType;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class FeeConfigRequest {
    @NotNull private Long productId;
    @NotNull private FeeType feeType;
    @NotNull private FeeMode feeMode;
    @NotNull @Positive private BigDecimal value;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
