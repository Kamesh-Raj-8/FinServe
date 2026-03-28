package com.smelend.smelendbackend.service.kyc;

import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.Role;
import com.smelend.smelendbackend.entity.enums.RoleName;

/**
 * Simple builders for unit tests ONLY.
 * Keeps entity construction readable in tests.
 */
class TestBuilders {

    static AppUser user(String roleName, Long userId, String email) {

        Role role = new Role();
        role.setRoleName(RoleName.valueOf(roleName));

        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setEmail(email);
        user.setRole(role);

        return user;
    }
}
