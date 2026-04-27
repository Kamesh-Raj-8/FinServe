package com.smelend.smelendbackend.dto.admin;

import com.smelend.smelendbackend.entity.enums.RoleName;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private RoleName role;
    private StatusFlag status;
    private String bankAccountNo;
    private String ifsc;
}