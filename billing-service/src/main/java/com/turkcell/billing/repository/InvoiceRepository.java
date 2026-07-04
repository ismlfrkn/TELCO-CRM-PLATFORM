package com.turkcell.billing.repository;

import com.turkcell.billing.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findAllByCustomerId(UUID customerId, Pageable pageable);

    Optional<Invoice> findBySubscriptionIdAndPeriodStartAndPeriodEnd(
            UUID subscriptionId, LocalDate periodStart, LocalDate periodEnd);
}
