package com.turkcell.usage.service;

import com.turkcell.usage.dto.request.CdrEventIngestRequest;
import com.turkcell.usage.dto.response.CdrEventResponse;
import com.turkcell.usage.entity.CdrEvent;
import com.turkcell.usage.entity.UsageRecord;
import com.turkcell.usage.exception.QuotaNotFoundException;
import com.turkcell.usage.exception.UnsupportedCdrTypeException;
import com.turkcell.usage.mapper.CdrEventMapper;
import com.turkcell.usage.mapper.UsageRecordMapper;
import com.turkcell.usage.repository.CdrEventRepository;
import com.turkcell.usage.repository.UsageRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class CdrEventService {

    private static final String AGGREGATE_CDR_EVENT = "CdrEvent";
    private static final String AGGREGATE_USAGE_RECORD = "UsageRecord";
    private static final String AGGREGATE_QUOTA = "Quota";

    private final CdrEventRepository cdrEventRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final QuotaService quotaService;
    private final CdrEventMapper cdrEventMapper;
    private final UsageRecordMapper usageRecordMapper;
    private final OutboxEventService outboxEventService;
    private final AuditLogService auditLogService;
    private final TransactionTemplate ingestTransactionTemplate;

    public CdrEventService(CdrEventRepository cdrEventRepository, UsageRecordRepository usageRecordRepository,
                            QuotaService quotaService, CdrEventMapper cdrEventMapper,
                            UsageRecordMapper usageRecordMapper, OutboxEventService outboxEventService,
                            AuditLogService auditLogService, PlatformTransactionManager transactionManager) {
        this.cdrEventRepository = cdrEventRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.quotaService = quotaService;
        this.cdrEventMapper = cdrEventMapper;
        this.usageRecordMapper = usageRecordMapper;
        this.outboxEventService = outboxEventService;
        this.auditLogService = auditLogService;

        this.ingestTransactionTemplate = new TransactionTemplate(transactionManager);
        this.ingestTransactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    public CdrEventResponse ingest(CdrEventIngestRequest request) {
        return cdrEventRepository.findByExternalCdrId(request.getExternalCdrId())
                .map(cdrEventMapper::toResponse)
                .orElseGet(() -> {
                    try {
                        return ingestTransactionTemplate.execute(status -> processNewCdrEvent(request));
                    } catch (DataIntegrityViolationException ex) {
                        return cdrEventRepository.findByExternalCdrId(request.getExternalCdrId())
                                .map(cdrEventMapper::toResponse)
                                .orElseThrow(() -> ex);
                    }
                });
    }

    private CdrEventResponse processNewCdrEvent(CdrEventIngestRequest request) {
        CdrEvent cdrEvent = new CdrEvent();
        cdrEvent.setExternalCdrId(request.getExternalCdrId());
        cdrEvent.setMsisdn(request.getMsisdn());
        cdrEvent.setCdrType(request.getCdrType());
        cdrEvent.setStartTime(request.getStartTime());
        cdrEvent.setEndTime(request.getEndTime());
        cdrEvent.setDurationSeconds(request.getDurationSeconds());
        cdrEvent.setDataVolumeBytes(request.getDataVolumeBytes());
        cdrEvent.setPartyB(request.getPartyB());
        cdrEvent.setNetworkType(request.getNetworkType());
        cdrEvent = cdrEventRepository.save(cdrEvent);

        try {
            BigDecimal quantity = computeQuantity(request.getCdrType(), request.getDurationSeconds(), request.getDataVolumeBytes());
            QuotaService.QuotaDeductionResult deduction =
                    quotaService.deduct(request.getSubscriptionId(), request.getCdrType(), quantity);

            UsageRecord usageRecord = new UsageRecord();
            usageRecord.setSubscriptionId(request.getSubscriptionId());
            usageRecord.setType(request.getCdrType());
            usageRecord.setQuantity(quantity);
            usageRecord.setCdrRef(request.getExternalCdrId());
            usageRecord = usageRecordRepository.save(usageRecord);

            cdrEvent.setStatus(CdrEvent.STATUS_PROCESSED);
            cdrEvent.setUsageRecordId(usageRecord.getId());
            cdrEvent.setProcessedAt(Instant.now());

            outboxEventService.publish(AGGREGATE_USAGE_RECORD, usageRecord.getId(), "UsageRecorded",
                    usageRecordMapper.toResponse(usageRecord));
            if (deduction.thresholdEvent() != null) {
                outboxEventService.publish(AGGREGATE_QUOTA, deduction.quota().getId(), deduction.thresholdEvent(), deduction.quota());
            }
            if (deduction.overageQuantity() != null) {
                outboxEventService.publish(AGGREGATE_QUOTA, deduction.quota().getId(), "UsageAggregated",
                        new UsageAggregatedPayload(deduction.quota().getSubscriptionId(), deduction.quota().getId(),
                                request.getCdrType(), deduction.overageQuantity(),
                                deduction.quota().getPeriodStart(), deduction.quota().getPeriodEnd()));
            }
            auditLogService.record("CDR_PROCESSED", AGGREGATE_CDR_EVENT, cdrEvent.getId(), null, usageRecordMapper.toResponse(usageRecord));
        } catch (QuotaNotFoundException | UnsupportedCdrTypeException ex) {
            cdrEvent.setStatus(CdrEvent.STATUS_FAILED);
            cdrEvent.setFailureReason(ex.getMessage());
            auditLogService.record("CDR_FAILED", AGGREGATE_CDR_EVENT, cdrEvent.getId(), null, ex.getMessage());
        }

        cdrEventRepository.save(cdrEvent);
        return cdrEventMapper.toResponse(cdrEvent);
    }

    private BigDecimal computeQuantity(String cdrType, Integer durationSeconds, Long dataVolumeBytes) {
        return switch (cdrType) {
            case "VOICE" -> {
                int seconds = durationSeconds != null ? durationSeconds : 0;
                yield BigDecimal.valueOf((seconds + 59) / 60);
            }
            case "SMS" -> BigDecimal.ONE;
            case "DATA" -> {
                long bytes = dataVolumeBytes != null ? dataVolumeBytes : 0L;
                yield BigDecimal.valueOf(bytes).divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.HALF_UP);
            }
            default -> throw new UnsupportedCdrTypeException("Unsupported cdr type: " + cdrType);
        };
    }

    public record UsageAggregatedPayload(UUID subscriptionId, UUID quotaId, String cdrType,
                                          BigDecimal overageQuantity, LocalDate periodStart, LocalDate periodEnd) {
    }
}
