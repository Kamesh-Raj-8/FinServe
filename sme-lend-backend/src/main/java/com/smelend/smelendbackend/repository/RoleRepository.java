package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.Role;
import com.smelend.smelendbackend.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(RoleName roleName);
}
