package com.turkcell.ticket.mapper;

import com.turkcell.ticket.dto.response.SlaResponse;
import com.turkcell.ticket.entity.Sla;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SlaMapper {
    SlaResponse toResponse(Sla sla);
}
