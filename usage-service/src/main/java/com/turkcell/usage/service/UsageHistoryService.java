package com.turkcell.usage.service;

import com.turkcell.usage.dto.response.UsageRecordResponse;
import com.turkcell.usage.mapper.UsageRecordMapper;
import com.turkcell.usage.repository.UsageRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class UsageHistoryService {

    private final UsageRecordRepository usageRecordRepository;
    private final UsageRecordMapper usageRecordMapper;

    public UsageHistoryService(UsageRecordRepository usageRecordRepository, UsageRecordMapper usageRecordMapper) {
        this.usageRecordRepository = usageRecordRepository;
        this.usageRecordMapper = usageRecordMapper;
    }

    public Page<UsageRecordResponse> getHistory(UUID subscriptionId, Instant from, Instant to, Pageable pageable) {
        return usageRecordRepository.findAllBySubscriptionIdAndRecordedAtBetween(subscriptionId, from, to, pageable)
                .map(usageRecordMapper::toResponse);
    }
}
