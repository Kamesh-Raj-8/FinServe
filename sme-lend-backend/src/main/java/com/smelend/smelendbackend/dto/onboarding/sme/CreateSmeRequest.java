package com.smelend.smelendbackend.dto.onboarding.sme;

import com.smelend.smelendbackend.entity.enums.BusinessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateSmeRequest {

    @NotBlank
    private String legalName;

    private String tradeName;

    private String registrationNo;

    @NotNull
    private BusinessType businessType;

    @NotBlank
    private String industry;

    @NotBlank
    private String address;

    private String gstNo;
}