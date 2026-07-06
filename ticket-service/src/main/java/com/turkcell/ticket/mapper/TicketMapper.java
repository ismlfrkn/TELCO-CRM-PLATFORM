package com.turkcell.ticket.mapper;

import com.turkcell.ticket.dto.response.TicketResponse;
import com.turkcell.ticket.entity.Ticket;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketMapper {
    TicketResponse toResponse(Ticket ticket);
}
