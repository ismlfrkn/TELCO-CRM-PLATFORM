package com.turkcell.subscription.service;

import com.turkcell.subscription.dto.request.SubscriptionCreateRequest;
import com.turkcell.subscription.dto.response.SubscriptionResponse;
import com.turkcell.subscription.entity.Subscription;
import com.turkcell.subscription.exception.InvalidSubscriptionTransitionException;
import com.turkcell.subscription.exception.SubscriptionNotFoundException;
import com.turkcell.subscription.mapper.SubscriptionMapper;
import com.turkcell.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SubscriptionServiceTest {

    private SubscriptionRepository subscriptionRepository;
    private MsisdnPoolService msisdnPoolService;
    private OutboxEventService outboxEventService;
    private AuditLogService auditLogService;
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(SubscriptionRepository.class);
        msisdnPoolService = mock(MsisdnPoolService.class);
        outboxEventService = mock(OutboxEventService.class);
        auditLogService = mock(AuditLogService.class);
        SubscriptionMapper subscriptionMapper = Mappers.getMapper(SubscriptionMapper.class);

        subscriptionService = new SubscriptionService(
                subscriptionRepository, msisdnPoolService, subscriptionMapper, outboxEventService, auditLogService);

        when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createSubscription_withoutMsisdn_allocatesNextFromPool() {
        SubscriptionCreateRequest request = new SubscriptionCreateRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setTariffCode("TARIFF-1");

        when(msisdnPoolService.allocateNext()).thenReturn("905550000001");

        SubscriptionResponse response = subscriptionService.createSubscription(request);

        assertThat(response.getMsisdn()).isEqualTo("905550000001");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getActivatedAt()).isNotNull();

        verify(msisdnPoolService, never()).allocateSpecific(any());
        verify(outboxEventService).publish(eq("Subscription"), any(), eq("SubscriptionActivated"), any());
        verify(auditLogService).record(eq("SUBSCRIPTION_ACTIVATED"), eq("Subscription"), any(), eq(null), any());
    }

    @Test
    void createSubscription_withExplicitMsisdn_allocatesSpecificNumber() {
        SubscriptionCreateRequest request = new SubscriptionCreateRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setTariffCode("TARIFF-1");
        request.setMsisdn("905550000099");

        when(msisdnPoolService.allocateSpecific("905550000099")).thenReturn("905550000099");

        SubscriptionResponse response = subscriptionService.createSubscription(request);

        assertThat(response.getMsisdn()).isEqualTo("905550000099");
        verify(msisdnPoolService, never()).allocateNext();
    }

    @Test
    void suspend_fromActive_succeedsAndPublishesEvent() {
        Subscription subscription = activeSubscription();
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.suspend(subscription.getId());

        assertThat(response.getStatus()).isEqualTo("SUSPENDED");
        verify(outboxEventService).publish(eq("Subscription"), eq(subscription.getId()), eq("SubscriptionSuspended"), any());
    }

    @Test
    void suspend_whenNotActive_throwsInvalidTransition() {
        Subscription subscription = activeSubscription();
        subscription.setStatus("SUSPENDED");
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.suspend(subscription.getId()))
                .isInstanceOf(InvalidSubscriptionTransitionException.class);
    }

    @Test
    void reactivate_fromSuspended_succeeds() {
        Subscription subscription = activeSubscription();
        subscription.setStatus("SUSPENDED");
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.reactivate(subscription.getId());

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(outboxEventService).publish(eq("Subscription"), eq(subscription.getId()), eq("SubscriptionActivated"), any());
    }

    @Test
    void reactivate_whenNotSuspended_throwsInvalidTransition() {
        Subscription subscription = activeSubscription();
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.reactivate(subscription.getId()))
                .isInstanceOf(InvalidSubscriptionTransitionException.class);
    }

    @Test
    void terminate_releasesMsisdnAndSetsTerminatedAt() {
        Subscription subscription = activeSubscription();
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.terminate(subscription.getId());

        assertThat(response.getStatus()).isEqualTo("TERMINATED");
        assertThat(response.getTerminatedAt()).isNotNull();
        verify(msisdnPoolService).release("905550000001");
        verify(outboxEventService).publish(eq("Subscription"), eq(subscription.getId()), eq("SubscriptionTerminated"), any());
    }

    @Test
    void terminate_whenAlreadyTerminated_throwsInvalidTransition() {
        Subscription subscription = activeSubscription();
        subscription.setStatus("TERMINATED");
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.terminate(subscription.getId()))
                .isInstanceOf(InvalidSubscriptionTransitionException.class);

        verify(msisdnPoolService, never()).release(any());
    }

    @Test
    void getSubscriptionById_whenMissing_throwsSubscriptionNotFoundException() {
        UUID id = UUID.randomUUID();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getSubscriptionById(id))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    private Subscription activeSubscription() {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setMsisdn("905550000001");
        subscription.setTariffCode("TARIFF-1");
        subscription.setStatus("ACTIVE");
        subscription.setActivatedAt(Instant.now());
        subscription.setCreatedAt(Instant.now());
        return subscription;
    }
}
