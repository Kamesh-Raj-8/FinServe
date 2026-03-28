package com.smelend.smelendbackend.dto.auth;

import com.smelend.smelendbackend.entity.enums.RoleName;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String phone;

    @NotNull
    private RoleName role;

    private String bankAccountNo;
    private String ifsc;

    @AssertTrue(message = "Only APPLICANT can self-register")
    public boolean isAllowedSelfRegisterRole() {
        return role == RoleName.APPLICANT;
    }
}