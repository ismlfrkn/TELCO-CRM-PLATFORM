package com.turkcell.identity.mapper;

import com.turkcell.identity.dto.response.PermissionResponse;
import com.turkcell.identity.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    PermissionResponse toResponse(Permission permission);
}
