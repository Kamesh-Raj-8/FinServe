package com.smelend.smelendbackend.service.admin;

import com.smelend.smelendbackend.dto.admin.RoleResponse;
import com.smelend.smelendbackend.mapper.RoleMapper;
import com.smelend.smelendbackend.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleAdminService {

    private final RoleRepository roleRepo;
    private final RoleMapper roleMapper;

    public RoleAdminService(RoleRepository roleRepo, RoleMapper roleMapper) {
        this.roleRepo = roleRepo;
        this.roleMapper = roleMapper;
    }

    public List<RoleResponse> listRoles() {
        return roleRepo.findAll().stream()
                .map(roleMapper::toResponse)
                .toList();
    }
}