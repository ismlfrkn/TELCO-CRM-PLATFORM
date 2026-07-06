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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TicketServiceTest {

    private TicketRepository ticketRepository;
    private SlaService slaService;
    private OutboxEventService outboxEventService;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        slaService = mock(SlaService.class);
        outboxEventService = mock(OutboxEventService.class);
        TicketMapper ticketMapper = Mappers.getMapper(TicketMapper.class);

        ticketService = new TicketService(ticketRepository, slaService, ticketMapper, outboxEventService);

        when(ticketRepository.save(any())).thenAnswer(inv -> {
            Ticket ticket = inv.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId(UUID.randomUUID());
            }
            return ticket;
        });
    }

    @Test
    void createTicket_resolvesSlaAndComputesSlaDueAt() {
        Sla sla = slaWith("TECHNICAL", "HIGH", 15, 240);
        when(slaService.getSlaByCategoryAndPriority("TECHNICAL", "HIGH")).thenReturn(sla);

        TicketCreateRequest request = new TicketCreateRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setCategory("TECHNICAL");
        request.setPriority("HIGH");

        Instant before = Instant.now();
        TicketResponse response = ticketService.createTicket(request);
        Instant expectedDueAt = before.plus(240, ChronoUnit.MINUTES);

        assertThat(response.getStatus()).isEqualTo(Ticket.STATUS_OPEN);
        assertThat(response.getSlaId()).isEqualTo(sla.getId());
        assertThat(response.getSlaDueAt()).isBetween(expectedDueAt.minusSeconds(5), expectedDueAt.plusSeconds(5));
        verify(outboxEventService).publish(eq("Ticket"), any(), eq("TicketOpened"), any());
    }

    @Test
    void createTicket_autoAssignsTeamBasedOnCategory() {
        when(slaService.getSlaByCategoryAndPriority(any(), any())).thenReturn(slaWith("BILLING", "MEDIUM", 30, 720));

        TicketCreateRequest request = new TicketCreateRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setCategory("BILLING");
        request.setPriority("MEDIUM");

        TicketResponse response = ticketService.createTicket(request);

        assertThat(response.getAssignedTeam()).isEqualTo("BILLING_SUPPORT");
    }

    @Test
    void createTicket_withUnknownCategory_assignsGeneralSupport() {
        when(slaService.getSlaByCategoryAndPriority(any(), any())).thenReturn(slaWith("OTHER", "LOW", 60, 1440));

        TicketCreateRequest request = new TicketCreateRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setCategory("OTHER");
        request.setPriority("LOW");

        TicketResponse response = ticketService.createTicket(request);

        assertThat(response.getAssignedTeam()).isEqualTo("GENERAL_SUPPORT");
    }

    @Test
    void assign_fromOpen_succeedsAndPublishesTicketAssigned() {
        Ticket ticket = openTicket();
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        TicketAssignRequest request = new TicketAssignRequest();
        request.setTeam("TECH_SUPPORT");

        TicketResponse response = ticketService.assign(ticket.getId(), request);

        assertThat(response.getStatus()).isEqualTo(Ticket.STATUS_ASSIGNED);
        assertThat(response.getAssignedTeam()).isEqualTo("TECH_SUPPORT");
        verify(outboxEventService).publish(eq("Ticket"), eq(ticket.getId()), eq("TicketAssigned"), any());
    }

    @Test
    void assign_onResolvedTicket_throwsInvalidTicketStateException() {
        Ticket ticket = openTicket();
        ticket.setStatus(Ticket.STATUS_RESOLVED);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        TicketAssignRequest request = new TicketAssignRequest();
        request.setTeam("TECH_SUPPORT");

        assertThatThrownBy(() -> ticketService.assign(ticket.getId(), request))
                .isInstanceOf(InvalidTicketStateException.class);
    }

    @Test
    void resolve_fromOpen_succeedsAndPublishesTicketResolved() {
        Ticket ticket = openTicket();
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.resolve(ticket.getId());

        assertThat(response.getStatus()).isEqualTo(Ticket.STATUS_RESOLVED);
        verify(outboxEventService).publish(eq("Ticket"), eq(ticket.getId()), eq("TicketResolved"), any());
    }

    @Test
    void resolve_whenAlreadyResolved_throwsInvalidTicketStateException() {
        Ticket ticket = openTicket();
        ticket.setStatus(Ticket.STATUS_RESOLVED);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.resolve(ticket.getId()))
                .isInstanceOf(InvalidTicketStateException.class);
    }

    @Test
    void markSlaBreached_whenDueDatePassed_publishesSlaBreachedEvent() {
        Ticket ticket = openTicket();
        ticket.setSlaDueAt(Instant.now().minusSeconds(60));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.markSlaBreached(ticket.getId());

        assertThat(response.getId()).isEqualTo(ticket.getId());
        verify(outboxEventService).publish(eq("Ticket"), eq(ticket.getId()), eq("SlaBreached"), any());
    }

    @Test
    void markSlaBreached_whenDueDateNotPassed_throwsInvalidTicketStateException() {
        Ticket ticket = openTicket();
        ticket.setSlaDueAt(Instant.now().plusSeconds(3600));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.markSlaBreached(ticket.getId()))
                .isInstanceOf(InvalidTicketStateException.class);
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void markSlaBreached_onResolvedTicket_throwsInvalidTicketStateException() {
        Ticket ticket = openTicket();
        ticket.setStatus(Ticket.STATUS_RESOLVED);
        ticket.setSlaDueAt(Instant.now().minusSeconds(60));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.markSlaBreached(ticket.getId()))
                .isInstanceOf(InvalidTicketStateException.class);
    }

    @Test
    void getTicketById_whenMissing_throwsTicketNotFoundException() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(id)).isInstanceOf(TicketNotFoundException.class);
    }

    private Sla slaWith(String category, String priority, int responseTime, int resolutionTime) {
        Sla sla = new Sla();
        sla.setId(UUID.randomUUID());
        sla.setCategory(category);
        sla.setPriority(priority);
        sla.setResponseTime(responseTime);
        sla.setResolutionTime(resolutionTime);
        return sla;
    }

    private Ticket openTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setCustomerId(UUID.randomUUID());
        ticket.setCategory("TECHNICAL");
        ticket.setPriority("HIGH");
        ticket.setStatus(Ticket.STATUS_OPEN);
        ticket.setSlaId(UUID.randomUUID());
        ticket.setSlaDueAt(Instant.now().plus(240, ChronoUnit.MINUTES));
        ticket.setCreatedAt(Instant.now());
        ticket.setAssignedTeam("TECH_SUPPORT");
        return ticket;
    }
}
