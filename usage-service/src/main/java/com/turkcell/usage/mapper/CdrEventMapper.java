package com.turkcell.usage.mapper;

import com.turkcell.usage.dto.response.CdrEventResponse;
import com.turkcell.usage.entity.CdrEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CdrEventMapper {
    CdrEventResponse toResponse(CdrEvent cdrEvent);
}
