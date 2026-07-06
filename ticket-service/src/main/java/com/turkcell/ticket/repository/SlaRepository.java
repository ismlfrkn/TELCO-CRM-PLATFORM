package com.turkcell.ticket.repository;

import com.turkcell.ticket.entity.Sla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlaRepository extends JpaRepository<Sla, UUID> {

    Optional<Sla> findByCategoryAndPriority(String category, String priority);

    boolean existsByCategoryAndPriority(String category, String priority);
}
