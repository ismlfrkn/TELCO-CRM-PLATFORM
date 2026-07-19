package com.turkcell.billing.repository;

import com.turkcell.billing.entity.UsageAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UsageAggregateRepository extends JpaRepository<UsageAggregate, UUID> {

    boolean existsBySourceEventId(UUID sourceEventId);

    List<UsageAggregate> findBySubscriptionIdAndInvoiceIdIsNull(UUID subscriptionId);
}
