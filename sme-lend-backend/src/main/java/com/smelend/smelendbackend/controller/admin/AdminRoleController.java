package com.smelend.smelendbackend.controller.admin;

import com.smelend.smelendbackend.dto.ApiResponse;
import com.smelend.smelendbackend.dto.admin.RoleResponse;
import com.smelend.smelendbackend.service.admin.RoleAdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController {

    private final RoleAdminService roleService;

    public AdminRoleController(RoleAdminService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.ok("Roles fetched", roleService.listRoles());
    }
}