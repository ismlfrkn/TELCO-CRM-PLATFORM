package com.turkcell.ticket.service;

import com.turkcell.ticket.dto.request.TicketAssignRequest;
import com.turkcell.ticket.dto.request.TicketCreateRequest;
import com.turkcell.ticket.dto.response.TicketResponse;
import com.turkcell.ticket.entity.Sla;
import com.turkcell.ticket.entity.Ticket;
import com.turkcell.ticket.exception.InvalidTicketStateException;
import com.turkcell.ticket.exception.TicketNotFoundException;
import com.turkcell.ticket.mapper.TicketMapper;
import com.turkcell.ticket.repository.TicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class TicketService {

    private static final String AGGREGATE_TYPE = "Ticket";

    private final TicketRepository ticketRepository;
    private final SlaService slaService;
    private final TicketMapper ticketMapper;
    private final OutboxEventService outboxEventService;

    public TicketService(TicketRepository ticketRepository, SlaService slaService, TicketMapper ticketMapper,
                          OutboxEventService outboxEventService) {
        this.ticketRepository = ticketRepository;
        this.slaService = slaService;
        this.ticketMapper = ticketMapper;
        this.outboxEventService = outboxEventService;
    }

    @Transactional
    public TicketResponse createTicket(TicketCreateRequest request) {
        Sla sla = slaService.getSlaByCategoryAndPriority(request.getCategory(), request.getPriority());

        Ticket ticket = new Ticket();
        ticket.setCustomerId(request.getCustomerId());
        ticket.setCategory(request.getCategory());
        ticket.setPriority(request.getPriority());
        ticket.setStatus(Ticket.STATUS_OPEN);
        ticket.setSlaId(sla.getId());
        ticket.setSlaDueAt(Instant.now().plus(sla.getResolutionTime(), ChronoUnit.MINUTES));

        ticket.setAssignedTeam(routeTeam(request.getCategory()));
        ticket = ticketRepository.save(ticket);

        TicketResponse response = ticketMapper.toResponse(ticket);
        outboxEventService.publish(AGGREGATE_TYPE, ticket.getId(), "TicketOpened", response);
        return response;
    }

    public TicketResponse getTicketResponseById(UUID id) {
        return ticketMapper.toResponse(getTicketById(id));
    }

    public Ticket getTicketById(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
    }

    public Page<TicketResponse> getTicketsByCustomer(UUID customerId, Pageable pageable) {
        return ticketRepository.findAllByCustomerId(customerId, pageable).map(ticketMapper::toResponse);
    }

    @Transactional
    public TicketResponse assign(UUID id, TicketAssignRequest request) {
        Ticket ticket = getTicketById(id);
        if (Ticket.STATUS_RESOLVED.equals(ticket.getStatus())) {
            throw new InvalidTicketStateException("Cannot reassign a RESOLVED ticket, id: " + id);
        }

        ticket.setAssignedTeam(request.getTeam());
        ticket.setStatus(Ticket.STATUS_ASSIGNED);
        ticketRepository.save(ticket);

        TicketResponse response = ticketMapper.toResponse(ticket);
        outboxEventService.publish(AGGREGATE_TYPE, ticket.getId(), "TicketAssigned", response);
        return response;
    }

    @Transactional
    public TicketResponse resolve(UUID id) {
        Ticket ticket = getTicketById(id);
        if (Ticket.STATUS_RESOLVED.equals(ticket.getStatus())) {
            throw new InvalidTicketStateException("Ticket is already RESOLVED, id: " + id);
        }

        ticket.setStatus(Ticket.STATUS_RESOLVED);
        ticketRepository.save(ticket);

        TicketResponse response = ticketMapper.toResponse(ticket);
        outboxEventService.publish(AGGREGATE_TYPE, ticket.getId(), "TicketResolved", response);
        return response;
    }

    @Transactional
    public TicketResponse markSlaBreached(UUID id) {
        Ticket ticket = getTicketById(id);
        if (Ticket.STATUS_RESOLVED.equals(ticket.getStatus())) {
            throw new InvalidTicketStateException("Cannot breach SLA on a RESOLVED ticket, id: " + id);
        }
        if (Instant.now().isBefore(ticket.getSlaDueAt())) {
            throw new InvalidTicketStateException("SLA due date has not passed yet for ticket, id: " + id);
        }

        TicketResponse response = ticketMapper.toResponse(ticket);
        outboxEventService.publish(AGGREGATE_TYPE, ticket.getId(), "SlaBreached", response);
        return response;
    }

    private String routeTeam(String category) {
        return switch (category) {
            case "TECHNICAL" -> "TECH_SUPPORT";
            case "BILLING" -> "BILLING_SUPPORT";
            case "COMPLAINT" -> "CUSTOMER_RELATIONS";
            default -> "GENERAL_SUPPORT";
        };
    }
}
