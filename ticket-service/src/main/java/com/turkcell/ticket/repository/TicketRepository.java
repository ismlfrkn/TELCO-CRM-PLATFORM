package com.turkcell.ticket.repository;

import com.turkcell.ticket.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Page<Ticket> findAllByCustomerId(UUID customerId, Pageable pageable);
}
