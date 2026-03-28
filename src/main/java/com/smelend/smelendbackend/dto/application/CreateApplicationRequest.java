package com.smelend.smelendbackend.dto.application;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@Data
@Builder
public class CreateApplicationRequest {

    @NotNull
    private Long smeId;

    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal requestedAmount;

    @NotNull
    @Min(1)
    private Integer tenorMonths;

    private String purposeNote;
}