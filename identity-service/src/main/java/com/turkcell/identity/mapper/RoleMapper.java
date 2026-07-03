package com.turkcell.identity.mapper;

import com.turkcell.identity.dto.response.RoleResponse;
import com.turkcell.identity.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", ignore = true)
    RoleResponse toResponse(Role role);
}
