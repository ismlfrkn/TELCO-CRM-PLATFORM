package com.turkcell.ticket.service;

import com.turkcell.ticket.dto.request.TicketCommentCreateRequest;
import com.turkcell.ticket.dto.response.TicketCommentResponse;
import com.turkcell.ticket.entity.Ticket;
import com.turkcell.ticket.entity.TicketComment;
import com.turkcell.ticket.exception.TicketNotFoundException;
import com.turkcell.ticket.mapper.TicketCommentMapper;
import com.turkcell.ticket.repository.TicketCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TicketCommentServiceTest {

    private TicketCommentRepository ticketCommentRepository;
    private TicketService ticketService;
    private TicketCommentService ticketCommentService;

    @BeforeEach
    void setUp() {
        ticketCommentRepository = mock(TicketCommentRepository.class);
        ticketService = mock(TicketService.class);
        TicketCommentMapper mapper = Mappers.getMapper(TicketCommentMapper.class);
        ticketCommentService = new TicketCommentService(ticketCommentRepository, ticketService, mapper);

        when(ticketCommentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void addComment_whenTicketExists_savesComment() {
        UUID ticketId = UUID.randomUUID();
        when(ticketService.getTicketById(ticketId)).thenReturn(new Ticket());

        TicketCommentCreateRequest request = new TicketCommentCreateRequest();
        request.setAuthorId(UUID.randomUUID());
        request.setBody("Musteri ile iletisime gecildi.");

        TicketCommentResponse response = ticketCommentService.addComment(ticketId, request);

        assertThat(response.getBody()).isEqualTo("Musteri ile iletisime gecildi.");
    }

    @Test
    void addComment_whenTicketMissing_throwsTicketNotFoundException() {
        UUID ticketId = UUID.randomUUID();
        when(ticketService.getTicketById(ticketId)).thenThrow(new TicketNotFoundException("Ticket not found with id: " + ticketId));

        TicketCommentCreateRequest request = new TicketCommentCreateRequest();
        request.setAuthorId(UUID.randomUUID());
        request.setBody("body");

        assertThatThrownBy(() -> ticketCommentService.addComment(ticketId, request))
                .isInstanceOf(TicketNotFoundException.class);
        verify(ticketCommentRepository, never()).save(any());
    }

    @Test
    void getComments_returnsMappedList() {
        UUID ticketId = UUID.randomUUID();
        when(ticketService.getTicketById(ticketId)).thenReturn(new Ticket());

        TicketComment comment = new TicketComment();
        comment.setId(UUID.randomUUID());
        comment.setTicketId(ticketId);
        comment.setAuthorId(UUID.randomUUID());
        comment.setBody("Ilk yorum");
        when(ticketCommentRepository.findAllByTicketIdOrderByCreatedAtAsc(ticketId)).thenReturn(List.of(comment));

        List<TicketCommentResponse> responses = ticketCommentService.getComments(ticketId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBody()).isEqualTo("Ilk yorum");
    }
}
