package com.smelend.smelendbackend.dto.underwriting;

import com.smelend.smelendbackend.entity.enums.UwDecision;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UwDecisionRequest {

    @NotNull
    private UwDecision decision; // APPROVE / REJECT / RETURN

    private String summaryNote;
}