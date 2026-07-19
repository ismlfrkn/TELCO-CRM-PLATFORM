package com.turkcell.usage.service;

import com.turkcell.usage.dto.request.QuotaCreateRequest;
import com.turkcell.usage.dto.response.QuotaResponse;
import com.turkcell.usage.entity.Quota;
import com.turkcell.usage.exception.DuplicateQuotaException;
import com.turkcell.usage.exception.QuotaNotFoundException;
import com.turkcell.usage.exception.UnsupportedCdrTypeException;
import com.turkcell.usage.mapper.QuotaMapper;
import com.turkcell.usage.repository.QuotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuotaServiceTest {

    private QuotaRepository quotaRepository;
    private QuotaService quotaService;

    @BeforeEach
    void setUp() {
        quotaRepository = mock(QuotaRepository.class);
        QuotaMapper quotaMapper = Mappers.getMapper(QuotaMapper.class);
        quotaService = new QuotaService(quotaRepository, quotaMapper);

        when(quotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createQuota_withNewSubscription_initializesRemainingFromIncluded() {
        UUID subscriptionId = UUID.randomUUID();
        when(quotaRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.empty());

        QuotaCreateRequest request = new QuotaCreateRequest();
        request.setSubscriptionId(subscriptionId);
        request.setPeriodStart(LocalDate.of(2026, 7, 1));
        request.setPeriodEnd(LocalDate.of(2026, 7, 31));
        request.setMinutesIncluded(1000);
        request.setSmsIncluded(1000);
        request.setMbIncluded(10240);

        QuotaResponse response = quotaService.createQuota(request);

        assertThat(response.getMinutesRemaining()).isEqualTo(1000);
        assertThat(response.getSmsRemaining()).isEqualTo(1000);
        assertThat(response.getMbRemaining()).isEqualTo(10240);
    }

    @Test
    void createQuota_whenSubscriptionAlreadyHasQuota_throwsDuplicateQuotaException() {
        UUID subscriptionId = UUID.randomUUID();
        when(quotaRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(new Quota()));

        QuotaCreateRequest request = new QuotaCreateRequest();
        request.setSubscriptionId(subscriptionId);
        request.setPeriodStart(LocalDate.now());
        request.setPeriodEnd(LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> quotaService.createQuota(request)).isInstanceOf(DuplicateQuotaException.class);
        verify(quotaRepository, never()).save(any());
    }

    @Test
    void getQuotaResponse_whenMissing_throwsQuotaNotFoundException() {
        UUID subscriptionId = UUID.randomUUID();
        when(quotaRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quotaService.getQuotaResponse(subscriptionId))
                .isInstanceOf(QuotaNotFoundException.class);
    }

    @Test
    void deduct_voiceCdr_decrementsMinutesRemaining() {
        UUID subscriptionId = UUID.randomUUID();
        Quota quota = quotaWith(100, 100, 100);
        when(quotaRepository.findBySubscriptionIdForUpdate(subscriptionId)).thenReturn(Optional.of(quota));

        QuotaService.QuotaDeductionResult result = quotaService.deduct(subscriptionId, "VOICE", BigDecimal.valueOf(10));

        assertThat(result.quota().getMinutesRemaining()).isEqualTo(90);
        assertThat(result.quota().getSmsRemaining()).isEqualTo(100);
    }

    @Test
    void deduct_smsCdr_decrementsSmsRemaining() {
        UUID subscriptionId = UUID.randomUUID();
        Quota quota = quotaWith(100, 100, 100);
        when(quotaRepository.findBySubscriptionIdForUpdate(subscriptionId)).thenReturn(Optional.of(quota));

        QuotaService.QuotaDeductionResult result = quotaService.deduct(subscriptionId, "SMS", BigDecimal.ONE);

        assertThat(result.quota().getSmsRemaining()).isEqualTo(99);
    }

    @Test
    void deduct_dataCdr_decrementsMbRemainingRoundedUp() {
        UUID subscriptionId = UUID.randomUUID();
        Quota quota = quotaWith(100, 100, 100);
        when(quotaRepository.findBySubscriptionIdForUpdate(subscriptionId)).thenReturn(Optional.of(quota));

        QuotaService.QuotaDeductionResult result = quotaService.deduct(subscriptionId, "DATA", BigDecimal.valueOf(5.2));

        assertThat(result.quota().getMbRemaining()).isEqualTo(94);
    }

    @Test
    void deduct_unsupportedCdrType_throwsUnsupportedCdrTypeException() {
        UUID subscriptionId = UUID.randomUUID();
        when(quotaRepository.findBySubscriptionIdForUpdate(subscriptionId)).thenReturn(Optional.of(quotaWith(100, 100, 100)));

        assertThatThrownBy(() -> quotaService.deduct(subscriptionId, "FAX", BigDecimal.ONE))
                .isInstanceOf(UnsupportedCdrTypeException.class);
    }

    @Test
    void deduct_whenQuotaMissing_throwsQuotaNotFoundException() {
        UUID subscriptionId = UUID.randomUUID();
        when(quotaRepository.findBySubscriptionIdForUpdate(subscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quotaService.deduct(subscriptionId, "VOICE", BigDecimal.TEN))
                .isInstanceOf(QuotaNotFoundException.class);
    }

    @Test
    void deduct_crossing80PercentThreshold_firesThresholdReachedEvent() {
        UUID subscriptionId = UUID.randomUUID();
        Quota quota = quotaWith(100, 0, 0);
        quota.setMinutesRemaining(25);

        when(quotaRepository.findBySubscriptionIdForUpdate(subscriptionId)).thenReturn(Optional.of(quota));

        QuotaService.QuotaDeductionResult result = quotaService.deduct(subscriptionId, "VOICE", BigDecimal.TEN);

        assertThat(result.quota().getMinutesRemaining()).isEqualTo(15);
        assertThat(result.thresholdEvent()).isEqualTo("QuotaThresholdReached");
        assertThat(result.overageQuantity()).isNull();
    }

    @Test
    void deduct_crossingZero_firesQuotaExceededEventInsteadOfThreshold() {
        UUID subscriptionId = UUID.randomUUID();
        Quota quota = quotaWith(100, 0, 0);
        quota.setMinutesRemaining(15);

        when(quotaRepository.findBySubscriptionIdForUpdate(subscriptionId)).thenReturn(Optional.of(quota));

        QuotaService.QuotaDeductionResult result = quotaService.deduct(subscriptionId, "VOICE", BigDecimal.valueOf(20));

        assertThat(result.quota().getMinutesRemaining()).isEqualTo(-5);
        assertThat(result.thresholdEvent()).isEqualTo("QuotaExceeded");

        assertThat(result.overageQuantity()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void deduct_alreadyBelowThreshold_doesNotRefireThresholdEvent() {
        UUID subscriptionId = UUID.randomUUID();
        Quota quota = quotaWith(100, 0, 0);
        quota.setMinutesRemaining(5);

        when(quotaRepository.findBySubscriptionIdForUpdate(subscriptionId)).thenReturn(Optional.of(quota));

        QuotaService.QuotaDeductionResult result = quotaService.deduct(subscriptionId, "VOICE", BigDecimal.valueOf(3));

        assertThat(result.quota().getMinutesRemaining()).isEqualTo(2);
        assertThat(result.thresholdEvent()).isNull();
    }

    @Test
    void deduct_alreadyExceeded_doesNotRefireExceededEvent() {
        UUID subscriptionId = UUID.randomUUID();
        Quota quota = quotaWith(100, 0, 0);
        quota.setMinutesRemaining(-5);

        when(quotaRepository.findBySubscriptionIdForUpdate(subscriptionId)).thenReturn(Optional.of(quota));

        QuotaService.QuotaDeductionResult result = quotaService.deduct(subscriptionId, "VOICE", BigDecimal.valueOf(3));

        assertThat(result.quota().getMinutesRemaining()).isEqualTo(-8);
        assertThat(result.thresholdEvent()).isNull();

        assertThat(result.overageQuantity()).isEqualByComparingTo(BigDecimal.valueOf(3));
    }

    private Quota quotaWith(int minutesIncluded, int smsIncluded, int mbIncluded) {
        Quota quota = new Quota();
        quota.setId(UUID.randomUUID());
        quota.setSubscriptionId(UUID.randomUUID());
        quota.setPeriodStart(LocalDate.now());
        quota.setPeriodEnd(LocalDate.now().plusDays(30));
        quota.setMinutesIncluded(minutesIncluded);
        quota.setSmsIncluded(smsIncluded);
        quota.setMbIncluded(mbIncluded);
        quota.setMinutesRemaining(minutesIncluded);
        quota.setSmsRemaining(smsIncluded);
        quota.setMbRemaining(mbIncluded);
        return quota;
    }
}
