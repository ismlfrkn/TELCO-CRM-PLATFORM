package com.turkcell.ticket.controller;

import com.turkcell.ticket.dto.request.TicketAssignRequest;
import com.turkcell.ticket.dto.request.TicketCommentCreateRequest;
import com.turkcell.ticket.dto.request.TicketCreateRequest;
import com.turkcell.ticket.dto.response.TicketCommentResponse;
import com.turkcell.ticket.dto.response.TicketResponse;
import com.turkcell.ticket.service.TicketCommentService;
import com.turkcell.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketCommentService ticketCommentService;

    public TicketController(TicketService ticketService, TicketCommentService ticketCommentService) {
        this.ticketService = ticketService;
        this.ticketCommentService = ticketCommentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody TicketCreateRequest request) {
        return ticketService.createTicket(request);
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable UUID id) {
        return ticketService.getTicketResponseById(id);
    }

    @GetMapping
    public Page<TicketResponse> getByCustomer(@RequestParam UUID customerId, Pageable pageable) {
        return ticketService.getTicketsByCustomer(customerId, pageable);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketCommentResponse addComment(@PathVariable UUID id, @Valid @RequestBody TicketCommentCreateRequest request) {
        return ticketCommentService.addComment(id, request);
    }

    @GetMapping("/{id}/comments")
    public List<TicketCommentResponse> getComments(@PathVariable UUID id) {
        return ticketCommentService.getComments(id);
    }

    @PostMapping("/{id}/assign")
    public TicketResponse assign(@PathVariable UUID id, @Valid @RequestBody TicketAssignRequest request) {
        return ticketService.assign(id, request);
    }

    @PostMapping("/{id}/resolve")
    public TicketResponse resolve(@PathVariable UUID id) {
        return ticketService.resolve(id);
    }

    @PostMapping("/{id}/mark-sla-breached")
    public TicketResponse markSlaBreached(@PathVariable UUID id) {
        return ticketService.markSlaBreached(id);
    }
}
