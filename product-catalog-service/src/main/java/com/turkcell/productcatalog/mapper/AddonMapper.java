package com.turkcell.productcatalog.mapper;

import com.turkcell.productcatalog.dto.response.AddonResponse;
import com.turkcell.productcatalog.entity.Addon;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddonMapper {
    AddonResponse toResponse(Addon addon);
}
