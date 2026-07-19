package com.turkcell.payment.service;

import com.turkcell.payment.entity.Payment;
import com.turkcell.payment.entity.PaymentAttempt;
import com.turkcell.payment.repository.PaymentAttemptRepository;
import com.turkcell.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentRetrySchedulerTest {

    private static final Instant NOW = Instant.parse("2026-07-15T12:00:00Z");

    private PaymentRepository paymentRepository;
    private PaymentAttemptRepository paymentAttemptRepository;
    private PaymentService paymentService;
    private PaymentRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentAttemptRepository = mock(PaymentAttemptRepository.class);
        paymentService = mock(PaymentService.class);
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);

        scheduler = new PaymentRetryScheduler(paymentRepository, paymentAttemptRepository, paymentService, fixedClock);
    }

    @Test
    void retryDuePayments_withNoFailedPayments_doesNothing() {
        when(paymentRepository.findAllByStatus("FAILED")).thenReturn(List.of());

        scheduler.retryDuePayments();

        verifyNoInteractions(paymentService);
    }

    @Test
    void retryDuePayments_whenFirstRetryIsDue_callsPaymentServiceRetry() {
        Payment payment = failedPayment();
        stubFailedPayments(payment);
        stubLastAttempt(payment.getId(), 1, NOW.minus(Duration.ofHours(25)));

        scheduler.retryDuePayments();

        verify(paymentService).retry(payment.getId());
    }

    @Test
    void retryDuePayments_whenFirstRetryNotYetDue_doesNotCallRetry() {
        Payment payment = failedPayment();
        stubFailedPayments(payment);
        stubLastAttempt(payment.getId(), 1, NOW.minus(Duration.ofHours(1)));

        scheduler.retryDuePayments();

        verify(paymentService, never()).retry(any());
    }

    @Test
    void retryDuePayments_whenSecondRetryIsDue_callsPaymentServiceRetry() {
        Payment payment = failedPayment();
        stubFailedPayments(payment);
        stubLastAttempt(payment.getId(), 2, NOW.minus(Duration.ofHours(73)));

        scheduler.retryDuePayments();

        verify(paymentService).retry(payment.getId());
    }

    @Test
    void retryDuePayments_whenSecondRetryNotYetDue_doesNotCallRetry() {
        Payment payment = failedPayment();
        stubFailedPayments(payment);

        stubLastAttempt(payment.getId(), 2, NOW.minus(Duration.ofHours(25)));

        scheduler.retryDuePayments();

        verify(paymentService, never()).retry(any());
    }

    @Test
    void retryDuePayments_whenThirdRetryIsDue_callsPaymentServiceRetry() {
        Payment payment = failedPayment();
        stubFailedPayments(payment);
        stubLastAttempt(payment.getId(), 3, NOW.minus(Duration.ofHours(169)));

        scheduler.retryDuePayments();

        verify(paymentService).retry(payment.getId());
    }

    @Test
    void retryDuePayments_whenAttemptsExhausted_doesNotRetryEvenIfLongOverdue() {
        Payment payment = failedPayment();
        stubFailedPayments(payment);
        stubLastAttempt(payment.getId(), 4, NOW.minus(Duration.ofDays(30)));

        scheduler.retryDuePayments();

        verify(paymentService, never()).retry(any());
    }

    @Test
    void retryDuePayments_whenNoAttemptRecordExists_skipsGracefully() {
        Payment payment = failedPayment();
        stubFailedPayments(payment);
        when(paymentAttemptRepository.findTopByPaymentIdOrderByAttemptNoDesc(payment.getId()))
                .thenReturn(Optional.empty());

        scheduler.retryDuePayments();

        verify(paymentService, never()).retry(any());
    }

    @Test
    void retryDuePayments_whenOnePaymentRetryThrows_stillProcessesTheOthers() {
        Payment failing = failedPayment();
        Payment succeeding = failedPayment();
        when(paymentRepository.findAllByStatus("FAILED")).thenReturn(List.of(failing, succeeding));
        stubLastAttempt(failing.getId(), 1, NOW.minus(Duration.ofHours(25)));
        stubLastAttempt(succeeding.getId(), 1, NOW.minus(Duration.ofHours(25)));
        doThrow(new RuntimeException("mock PSP unavailable")).when(paymentService).retry(failing.getId());

        scheduler.retryDuePayments();

        verify(paymentService).retry(failing.getId());
        verify(paymentService).retry(succeeding.getId());
    }

    private Payment failedPayment() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setStatus("FAILED");
        return payment;
    }

    private void stubFailedPayments(Payment... payments) {
        when(paymentRepository.findAllByStatus("FAILED")).thenReturn(List.of(payments));
    }

    private void stubLastAttempt(UUID paymentId, int attemptNo, Instant attemptedAt) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setPaymentId(paymentId);
        attempt.setAttemptNo(attemptNo);
        attempt.setAttemptedAt(attemptedAt);
        when(paymentAttemptRepository.findTopByPaymentIdOrderByAttemptNoDesc(eq(paymentId)))
                .thenReturn(Optional.of(attempt));
    }
}
