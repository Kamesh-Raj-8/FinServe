package com.smelend.smelendbackend.dto.admin;

import com.smelend.smelendbackend.entity.enums.RoleName;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RoleResponse {
    private Long roleId;
    private RoleName roleName;
    private StatusFlag status;
}