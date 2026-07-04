package com.turkcell.usage.repository;

import com.turkcell.usage.entity.UsageRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {

    Page<UsageRecord> findAllBySubscriptionIdAndRecordedAtBetween(
            UUID subscriptionId, Instant from, Instant to, Pageable pageable);
}
