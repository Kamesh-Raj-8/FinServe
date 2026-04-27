package com.smelend.smelendbackend.dto.onboarding.promoter;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddPromoterRequest {

    @NotBlank
    private String promoterName;

    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
    private String mobile;

    /** Optional contact email */
    private String email;

    @NotNull
    @DecimalMin("0.01") @DecimalMax("100.00")
    private java.math.BigDecimal ownershipPct;

    /** PAN card number (e.g. ABCDE1234F) */
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Enter a valid PAN number")
    private String panNumber;

    /** Aadhaar number (12 digits) */
    @Pattern(regexp = "^\\d{12}$", message = "Aadhaar must be 12 digits")
    private String aadhaarNumber;

    /** DIN — Director Identification Number (optional) */
    private String din;

    /** Date of birth (ISO format: yyyy-MM-dd) */
    private String dateOfBirth;
}
