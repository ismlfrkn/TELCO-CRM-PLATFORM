package com.turkcell.usage.mapper;

import com.turkcell.usage.dto.response.QuotaResponse;
import com.turkcell.usage.entity.Quota;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuotaMapper {
    QuotaResponse toResponse(Quota quota);
}
