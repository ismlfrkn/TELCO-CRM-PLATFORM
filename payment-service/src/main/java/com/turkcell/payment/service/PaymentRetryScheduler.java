package com.turkcell.payment.service;

import com.turkcell.payment.entity.Payment;
import com.turkcell.payment.entity.PaymentAttempt;
import com.turkcell.payment.repository.PaymentAttemptRepository;
import com.turkcell.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class PaymentRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentRetryScheduler.class);
    private static final String STATUS_FAILED = "FAILED";
    private static final Duration[] RETRY_DELAYS = {Duration.ofHours(24), Duration.ofHours(72), Duration.ofHours(168)};
    private static final int MAX_ATTEMPTS = RETRY_DELAYS.length + 1;

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentService paymentService;
    private final Clock clock;

    public PaymentRetryScheduler(PaymentRepository paymentRepository, PaymentAttemptRepository paymentAttemptRepository,
                                  PaymentService paymentService, Clock clock) {
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.paymentService = paymentService;
        this.clock = clock;
    }

    @Scheduled(cron = "${payment.retry.cron:0 0 * * * *}")
    public void retryDuePayments() {
        List<Payment> failedPayments = paymentRepository.findAllByStatus(STATUS_FAILED);
        for (Payment payment : failedPayments) {
            retryIfDue(payment);
        }
    }

    private void retryIfDue(Payment payment) {
        Optional<PaymentAttempt> lastAttempt =
                paymentAttemptRepository.findTopByPaymentIdOrderByAttemptNoDesc(payment.getId());
        if (lastAttempt.isEmpty() || lastAttempt.get().getAttemptNo() >= MAX_ATTEMPTS) {
            return;
        }

        PaymentAttempt attempt = lastAttempt.get();
        Duration delay = RETRY_DELAYS[attempt.getAttemptNo() - 1];
        Instant dueAt = attempt.getAttemptedAt().plus(delay);
        if (Instant.now(clock).isBefore(dueAt)) {
            return;
        }

        try {
            paymentService.retry(payment.getId());
        } catch (RuntimeException ex) {

            log.warn("Scheduled retry failed for payment {}: {}", payment.getId(), ex.getMessage());
        }
    }
}
