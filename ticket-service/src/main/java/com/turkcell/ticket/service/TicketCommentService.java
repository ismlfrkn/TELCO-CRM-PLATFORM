package com.turkcell.ticket.service;

import com.turkcell.ticket.dto.request.TicketCommentCreateRequest;
import com.turkcell.ticket.dto.response.TicketCommentResponse;
import com.turkcell.ticket.entity.TicketComment;
import com.turkcell.ticket.mapper.TicketCommentMapper;
import com.turkcell.ticket.repository.TicketCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketCommentService {

    private final TicketCommentRepository ticketCommentRepository;
    private final TicketService ticketService;
    private final TicketCommentMapper ticketCommentMapper;

    public TicketCommentService(TicketCommentRepository ticketCommentRepository, TicketService ticketService,
                                 TicketCommentMapper ticketCommentMapper) {
        this.ticketCommentRepository = ticketCommentRepository;
        this.ticketService = ticketService;
        this.ticketCommentMapper = ticketCommentMapper;
    }

    @Transactional
    public TicketCommentResponse addComment(UUID ticketId, TicketCommentCreateRequest request) {
        ticketService.getTicketById(ticketId); // ticket yoksa TicketNotFoundException firlatir

        TicketComment comment = new TicketComment();
        comment.setTicketId(ticketId);
        comment.setAuthorId(request.getAuthorId());
        comment.setBody(request.getBody());

        return ticketCommentMapper.toResponse(ticketCommentRepository.save(comment));
    }

    public List<TicketCommentResponse> getComments(UUID ticketId) {
        ticketService.getTicketById(ticketId);
        return ticketCommentRepository.findAllByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(ticketCommentMapper::toResponse)
                .collect(Collectors.toList());
    }
}
