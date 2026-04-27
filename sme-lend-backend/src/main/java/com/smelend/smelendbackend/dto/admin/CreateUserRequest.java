package com.smelend.smelendbackend.dto.admin;

import com.smelend.smelendbackend.entity.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateUserRequest {

    @NotBlank
    private String fullName;

    @Email @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String phone;

    @NotNull
    private RoleName role; // ADMIN creates internal roles too

    private String bankAccountNo;
    private String ifsc;
}