package com.turkcell.identity.mapper;

import com.turkcell.identity.dto.response.UserResponse;
import com.turkcell.identity.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", ignore = true)
    UserResponse toResponse(User user);
}
