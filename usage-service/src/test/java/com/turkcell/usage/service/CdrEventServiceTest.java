package com.turkcell.usage.service;

import com.turkcell.usage.dto.request.CdrEventIngestRequest;
import com.turkcell.usage.dto.response.CdrEventResponse;
import com.turkcell.usage.dto.response.QuotaResponse;
import com.turkcell.usage.entity.CdrEvent;
import com.turkcell.usage.exception.QuotaNotFoundException;
import com.turkcell.usage.mapper.CdrEventMapper;
import com.turkcell.usage.mapper.UsageRecordMapper;
import com.turkcell.usage.repository.CdrEventRepository;
import com.turkcell.usage.repository.UsageRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CdrEventServiceTest {

    private CdrEventRepository cdrEventRepository;
    private UsageRecordRepository usageRecordRepository;
    private QuotaService quotaService;
    private OutboxEventService outboxEventService;
    private AuditLogService auditLogService;
    private CdrEventService cdrEventService;

    @BeforeEach
    void setUp() {
        cdrEventRepository = mock(CdrEventRepository.class);
        usageRecordRepository = mock(UsageRecordRepository.class);
        quotaService = mock(QuotaService.class);
        outboxEventService = mock(OutboxEventService.class);
        auditLogService = mock(AuditLogService.class);
        CdrEventMapper cdrEventMapper = Mappers.getMapper(CdrEventMapper.class);
        UsageRecordMapper usageRecordMapper = Mappers.getMapper(UsageRecordMapper.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

        cdrEventService = new CdrEventService(cdrEventRepository, usageRecordRepository, quotaService,
                cdrEventMapper, usageRecordMapper, outboxEventService, auditLogService, transactionManager);

        when(cdrEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usageRecordRepository.save(any())).thenAnswer(inv -> {
            com.turkcell.usage.entity.UsageRecord usageRecord = inv.getArgument(0);
            if (usageRecord.getId() == null) {
                usageRecord.setId(UUID.randomUUID());
            }
            return usageRecord;
        });
    }

    @Test
    void ingest_newVoiceCdr_computesMinutesRoundedUpAndPublishesUsageRecorded() {
        UUID subscriptionId = UUID.randomUUID();
        when(cdrEventRepository.findByExternalCdrId("CDR-1")).thenReturn(Optional.empty());

        QuotaResponse quotaResponse = new QuotaResponse();
        quotaResponse.setId(UUID.randomUUID());
        when(quotaService.deduct(eq(subscriptionId), eq("VOICE"), eq(BigDecimal.valueOf(2))))
                .thenReturn(new QuotaService.QuotaDeductionResult(quotaResponse, null, null));

        CdrEventIngestRequest request = new CdrEventIngestRequest();
        request.setExternalCdrId("CDR-1");
        request.setSubscriptionId(subscriptionId);
        request.setMsisdn("905550000001");
        request.setCdrType("VOICE");
        request.setStartTime(Instant.now());
        request.setDurationSeconds(61);

        CdrEventResponse response = cdrEventService.ingest(request);

        assertThat(response.getStatus()).isEqualTo(CdrEvent.STATUS_PROCESSED);
        assertThat(response.getUsageRecordId()).isNotNull();
        verify(outboxEventService).publish(eq("UsageRecord"), any(), eq("UsageRecorded"), any());
        verify(outboxEventService, never()).publish(eq("Quota"), any(), any(), any());
    }

    @Test
    void ingest_smsCdr_computesQuantityAsOne() {
        UUID subscriptionId = UUID.randomUUID();
        when(cdrEventRepository.findByExternalCdrId("CDR-2")).thenReturn(Optional.empty());
        when(quotaService.deduct(eq(subscriptionId), eq("SMS"), eq(BigDecimal.ONE)))
                .thenReturn(new QuotaService.QuotaDeductionResult(new QuotaResponse(), null, null));

        CdrEventIngestRequest request = new CdrEventIngestRequest();
        request.setExternalCdrId("CDR-2");
        request.setSubscriptionId(subscriptionId);
        request.setMsisdn("905550000001");
        request.setCdrType("SMS");
        request.setStartTime(Instant.now());

        CdrEventResponse response = cdrEventService.ingest(request);

        assertThat(response.getStatus()).isEqualTo(CdrEvent.STATUS_PROCESSED);
        verify(quotaService).deduct(subscriptionId, "SMS", BigDecimal.ONE);
    }

    @Test
    void ingest_dataCdr_convertsBytesToMegabytes() {
        UUID subscriptionId = UUID.randomUUID();
        when(cdrEventRepository.findByExternalCdrId("CDR-3")).thenReturn(Optional.empty());
        when(quotaService.deduct(eq(subscriptionId), eq("DATA"), any()))
                .thenReturn(new QuotaService.QuotaDeductionResult(new QuotaResponse(), null, null));

        CdrEventIngestRequest request = new CdrEventIngestRequest();
        request.setExternalCdrId("CDR-3");
        request.setSubscriptionId(subscriptionId);
        request.setMsisdn("905550000001");
        request.setCdrType("DATA");
        request.setStartTime(Instant.now());
        request.setDataVolumeBytes(5_000_000L);

        cdrEventService.ingest(request);

        verify(quotaService).deduct(subscriptionId, "DATA", new BigDecimal("5.00"));
    }

    @Test
    void ingest_withSameExternalCdrId_returnsExistingWithoutReprocessing() {
        CdrEvent existing = new CdrEvent();
        existing.setId(UUID.randomUUID());
        existing.setExternalCdrId("CDR-DUP");
        existing.setStatus(CdrEvent.STATUS_PROCESSED);
        when(cdrEventRepository.findByExternalCdrId("CDR-DUP")).thenReturn(Optional.of(existing));

        CdrEventIngestRequest request = new CdrEventIngestRequest();
        request.setExternalCdrId("CDR-DUP");
        request.setSubscriptionId(UUID.randomUUID());
        request.setMsisdn("905550000001");
        request.setCdrType("SMS");
        request.setStartTime(Instant.now());

        CdrEventResponse response = cdrEventService.ingest(request);

        assertThat(response.getId()).isEqualTo(existing.getId());
        verifyNoInteractions(quotaService);
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void ingest_whenQuotaMissing_marksCdrAsFailedInsteadOfThrowing() {
        UUID subscriptionId = UUID.randomUUID();
        when(cdrEventRepository.findByExternalCdrId("CDR-4")).thenReturn(Optional.empty());
        when(quotaService.deduct(eq(subscriptionId), any(), any()))
                .thenThrow(new QuotaNotFoundException("Quota not found for subscription: " + subscriptionId));

        CdrEventIngestRequest request = new CdrEventIngestRequest();
        request.setExternalCdrId("CDR-4");
        request.setSubscriptionId(subscriptionId);
        request.setMsisdn("905550000001");
        request.setCdrType("SMS");
        request.setStartTime(Instant.now());

        CdrEventResponse response = cdrEventService.ingest(request);

        assertThat(response.getStatus()).isEqualTo(CdrEvent.STATUS_FAILED);
        assertThat(response.getFailureReason()).contains("Quota not found");
        verify(usageRecordRepository, never()).save(any());
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void ingest_whenDeductionCrossesThreshold_publishesQuotaThresholdEvent() {
        UUID subscriptionId = UUID.randomUUID();
        when(cdrEventRepository.findByExternalCdrId("CDR-5")).thenReturn(Optional.empty());

        QuotaResponse quotaResponse = new QuotaResponse();
        quotaResponse.setId(UUID.randomUUID());
        when(quotaService.deduct(eq(subscriptionId), eq("VOICE"), any()))
                .thenReturn(new QuotaService.QuotaDeductionResult(quotaResponse, "QuotaThresholdReached", null));

        CdrEventIngestRequest request = new CdrEventIngestRequest();
        request.setExternalCdrId("CDR-5");
        request.setSubscriptionId(subscriptionId);
        request.setMsisdn("905550000001");
        request.setCdrType("VOICE");
        request.setStartTime(Instant.now());
        request.setDurationSeconds(120);

        cdrEventService.ingest(request);

        verify(outboxEventService).publish(eq("Quota"), eq(quotaResponse.getId()), eq("QuotaThresholdReached"), any());
    }

    @Test
    void ingest_whenDeductionCausesOverage_publishesUsageAggregatedEvent() {
        UUID subscriptionId = UUID.randomUUID();
        when(cdrEventRepository.findByExternalCdrId("CDR-6")).thenReturn(Optional.empty());

        QuotaResponse quotaResponse = new QuotaResponse();
        quotaResponse.setId(UUID.randomUUID());
        when(quotaService.deduct(eq(subscriptionId), eq("VOICE"), any()))
                .thenReturn(new QuotaService.QuotaDeductionResult(quotaResponse, "QuotaExceeded", BigDecimal.valueOf(5)));

        CdrEventIngestRequest request = new CdrEventIngestRequest();
        request.setExternalCdrId("CDR-6");
        request.setSubscriptionId(subscriptionId);
        request.setMsisdn("905550000001");
        request.setCdrType("VOICE");
        request.setStartTime(Instant.now());
        request.setDurationSeconds(1200);

        cdrEventService.ingest(request);

        verify(outboxEventService).publish(eq("Quota"), eq(quotaResponse.getId()), eq("UsageAggregated"), any());
    }

    @Test
    void ingest_whenDeductionStaysWithinQuota_doesNotPublishUsageAggregatedEvent() {
        UUID subscriptionId = UUID.randomUUID();
        when(cdrEventRepository.findByExternalCdrId("CDR-7")).thenReturn(Optional.empty());
        when(quotaService.deduct(eq(subscriptionId), eq("VOICE"), any()))
                .thenReturn(new QuotaService.QuotaDeductionResult(new QuotaResponse(), null, null));

        CdrEventIngestRequest request = new CdrEventIngestRequest();
        request.setExternalCdrId("CDR-7");
        request.setSubscriptionId(subscriptionId);
        request.setMsisdn("905550000001");
        request.setCdrType("VOICE");
        request.setStartTime(Instant.now());
        request.setDurationSeconds(60);

        cdrEventService.ingest(request);

        verify(outboxEventService, never()).publish(eq("Quota"), any(), eq("UsageAggregated"), any());
    }
}
