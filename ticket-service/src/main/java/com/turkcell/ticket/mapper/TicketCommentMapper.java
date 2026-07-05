package com.turkcell.ticket.mapper;

import com.turkcell.ticket.dto.response.TicketCommentResponse;
import com.turkcell.ticket.entity.TicketComment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketCommentMapper {
    TicketCommentResponse toResponse(TicketComment ticketComment);
}
