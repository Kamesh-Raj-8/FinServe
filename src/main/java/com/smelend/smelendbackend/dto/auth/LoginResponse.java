package com.smelend.smelendbackend.dto.auth;

import com.smelend.smelendbackend.entity.enums.RoleName;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {
    private Long userId;
    private String email;
    private RoleName role;
    private String token;
}
