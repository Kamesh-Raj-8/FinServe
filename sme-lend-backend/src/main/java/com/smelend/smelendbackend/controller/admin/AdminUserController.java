package com.smelend.smelendbackend.controller.admin;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.admin.CreateUserRequest;
import com.smelend.smelendbackend.dto.admin.UserResponse;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import com.smelend.smelendbackend.service.admin.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin User Management — ADMIN role only.
 * ADMIN cannot create ADMIN or APPLICANT users via this endpoint.
 */
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ApiResponse.ok("User created", userAdminService.createUser(req));
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> listUsers() {
        return ApiResponse.ok("Users fetched", userAdminService.listUsers());
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<UserResponse> setStatus(
            @PathVariable Long userId,
            @RequestParam StatusFlag status
    ) {
        return ApiResponse.ok("User status updated", userAdminService.setStatus(userId, status));
    }
}
