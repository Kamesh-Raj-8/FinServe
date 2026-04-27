package com.smelend.smelendbackend.service.admin;

import com.smelend.smelendbackend.dto.admin.CreateUserRequest;
import com.smelend.smelendbackend.dto.admin.UserResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.Role;
import com.smelend.smelendbackend.entity.enums.RoleName;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.AppUserRepository;
import com.smelend.smelendbackend.repository.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-only service for user management.
 * Enforces: ADMIN cannot create ADMIN or APPLICANT users.
 */
@Service
public class UserAdminService {

    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(AppUserRepository userRepo,
                             RoleRepository roleRepo,
                             PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(CreateUserRequest req) {
        // Business rule: ADMIN cannot create ADMIN or APPLICANT via this endpoint
        if (req.getRole() == RoleName.ADMIN || req.getRole() == RoleName.APPLICANT) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot create users with ADMIN or APPLICANT role from this endpoint");
        }

        userRepo.findByEmail(req.getEmail()).ifPresent(u -> {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered: " + req.getEmail());
        });

        Role role = roleRepo.findByRoleName(req.getRole())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Role not found: " + req.getRole()));

        AppUser user = AppUser.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(role)
                .bankAccountNo(req.getBankAccountNo())
                .ifsc(req.getIfsc())
                .status(StatusFlag.ACTIVE)
                .build();

        return toDto(userRepo.save(user));
    }

    public List<UserResponse> listUsers() {
        return userRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public UserResponse setStatus(Long userId, StatusFlag status) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        // Business rule: Admin accounts can never be deactivated
        if (user.getRole() != null
                && user.getRole().getRoleName() == RoleName.ADMIN
                && status == StatusFlag.INACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Admin accounts cannot be deactivated. Remove or reassign the role first.");
        }

        user.setStatus(status);
        return toDto(userRepo.save(user));
    }

    private UserResponse toDto(AppUser u) {
        return UserResponse.builder()
                .userId(u.getUserId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole().getRoleName())
                .status(u.getStatus())
                .bankAccountNo(u.getBankAccountNo())
                .ifsc(u.getIfsc())
                .build();
    }
}
