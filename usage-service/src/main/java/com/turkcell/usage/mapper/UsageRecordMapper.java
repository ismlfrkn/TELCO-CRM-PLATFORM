package com.turkcell.usage.mapper;

import com.turkcell.usage.dto.response.UsageRecordResponse;
import com.turkcell.usage.entity.UsageRecord;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsageRecordMapper {
    UsageRecordResponse toResponse(UsageRecord usageRecord);
}
