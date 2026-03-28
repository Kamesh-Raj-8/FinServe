package com.smelend.smelendbackend.mapper;

import com.smelend.smelendbackend.dto.admin.RoleResponse;
import com.smelend.smelendbackend.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoleMapper {
    RoleResponse toResponse(Role role);
}