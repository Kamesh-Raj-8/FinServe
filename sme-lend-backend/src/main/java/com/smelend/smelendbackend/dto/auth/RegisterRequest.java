package com.smelend.smelendbackend.dto.auth;

import com.smelend.smelendbackend.entity.enums.RoleName;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {

    @NotBlank
    private String fullName;

    @Email @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Phone must be a valid 10-digit Indian mobile number")
    private String phone;

    @NotNull
    private RoleName role;

    /**
     * Applicant bank account number — mandatory for loan disbursement.
     * Used as the basis for Loan Account Number generation.
     * Format: 9–18 digits (Indian bank account numbers).
     */
    @NotBlank(message = "Bank account number is required")
    @Pattern(regexp = "^[0-9]{9,18}$",
             message = "Bank account number must be 9–18 digits")
    private String bankAccountNo;

    /**
     * IFSC code — mandatory for NEFT/IMPS disbursement.
     * Format: 4 uppercase letters (bank code) + 0 + 6 alphanumeric chars.
     * Example: SBIN0001234
     */
    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$",
             message = "IFSC must be in format: 4 letters + 0 + 6 alphanumerics (e.g. SBIN0001234)")
    private String ifsc;

    @AssertTrue(message = "Only APPLICANT can self-register")
    public boolean isAllowedSelfRegisterRole() {
        return role == RoleName.APPLICANT;
    }
}
