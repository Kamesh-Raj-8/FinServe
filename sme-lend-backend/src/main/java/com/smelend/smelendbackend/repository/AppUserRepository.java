package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    @Query("SELECT u FROM AppUser u WHERE u.role.roleName = :role AND u.status = 'ACTIVE'")
    List<AppUser> findActiveByRole(@Param("role") RoleName role);

    Optional<AppUser> findByUserId(Long userId);
}
