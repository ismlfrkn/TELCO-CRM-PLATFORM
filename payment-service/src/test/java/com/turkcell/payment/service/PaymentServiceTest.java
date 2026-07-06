package com.turkcell.payment.service;

import com.turkcell.payment.dto.request.PaymentCreateRequest;
import com.turkcell.payment.dto.response.PaymentResponse;
import com.turkcell.payment.entity.Payment;
import com.turkcell.payment.exception.InsufficientWalletBalanceException;
import com.turkcell.payment.exception.InvalidPaymentStateException;
import com.turkcell.payment.exception.MissingWalletIdException;
import com.turkcell.payment.exception.PaymentNotFoundException;
import com.turkcell.payment.mapper.PaymentMapper;
import com.turkcell.payment.repository.PaymentAttemptRepository;
import com.turkcell.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private PaymentAttemptRepository paymentAttemptRepository;
    private WalletService walletService;
    private OutboxEventService outboxEventService;
    private AuditLogService auditLogService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentAttemptRepository = mock(PaymentAttemptRepository.class);
        walletService = mock(WalletService.class);
        outboxEventService = mock(OutboxEventService.class);
        auditLogService = mock(AuditLogService.class);
        PaymentMapper paymentMapper = Mappers.getMapper(PaymentMapper.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

        paymentService = new PaymentService(paymentRepository, paymentAttemptRepository, walletService,
                paymentMapper, outboxEventService, auditLogService, transactionManager);

        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentAttemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createPayment_withCardMethod_succeedsAndPublishesCompletedEvent() {
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        PaymentCreateRequest request = cardRequest(new BigDecimal("100.00"));

        PaymentResponse response = paymentService.createPayment("key-1", request);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getExternalRef()).isNotNull();
        assertThat(response.getPaidAt()).isNotNull();
        verify(outboxEventService).publish(eq("Payment"), any(), eq("PaymentCompleted"), any());
        verifyNoInteractions(walletService);
    }

    @Test
    void createPayment_withExplicitCurrency_propagatesCurrencyToPayment() {
        when(paymentRepository.findByIdempotencyKey("key-eur")).thenReturn(Optional.empty());
        PaymentCreateRequest request = cardRequest(new BigDecimal("100.00"));
        request.setCurrency("EUR");

        PaymentResponse response = paymentService.createPayment("key-eur", request);

        assertThat(response.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void createPayment_withSimulateFailure_marksFailedAndPublishesFailedEvent() {
        when(paymentRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        PaymentCreateRequest request = cardRequest(new BigDecimal("100.00"));
        request.setSimulateFailure(true);

        PaymentResponse response = paymentService.createPayment("key-2", request);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getPaidAt()).isNull();
        verify(outboxEventService).publish(eq("Payment"), any(), eq("PaymentFailed"), any());
    }

    @Test
    void createPayment_withSameIdempotencyKey_returnsExistingPaymentWithoutReprocessing() {
        Payment existing = new Payment();
        existing.setId(UUID.randomUUID());
        existing.setStatus("COMPLETED");
        existing.setIdempotencyKey("dup-key");
        when(paymentRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));

        PaymentResponse response = paymentService.createPayment("dup-key", cardRequest(new BigDecimal("50.00")));

        assertThat(response.getId()).isEqualTo(existing.getId());
        verify(paymentAttemptRepository, never()).save(any());
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void createPayment_withWalletMethodAndInsufficientBalance_marksFailed() {
        when(paymentRepository.findByIdempotencyKey("key-3")).thenReturn(Optional.empty());
        UUID walletId = UUID.randomUUID();
        doThrow(new InsufficientWalletBalanceException("insufficient"))
                .when(walletService).debit(eq(walletId), any());

        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setInvoiceId(UUID.randomUUID());
        request.setAmount(new BigDecimal("100.00"));
        request.setMethod("WALLET");
        request.setWalletId(walletId);

        PaymentResponse response = paymentService.createPayment("key-3", request);

        assertThat(response.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void createPayment_withWalletMethodMissingWalletId_throwsMissingWalletIdException() {
        when(paymentRepository.findByIdempotencyKey("key-4")).thenReturn(Optional.empty());
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setInvoiceId(UUID.randomUUID());
        request.setAmount(new BigDecimal("100.00"));
        request.setMethod("WALLET");

        assertThatThrownBy(() -> paymentService.createPayment("key-4", request))
                .isInstanceOf(MissingWalletIdException.class);

        verifyNoInteractions(walletService);
    }

    @Test
    void retry_onFailedPayment_succeedsAndRecordsNextAttemptNumber() {
        Payment payment = completedOrFailedPayment("FAILED");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentAttemptRepository.countByPaymentId(payment.getId())).thenReturn(1);

        PaymentResponse response = paymentService.retry(payment.getId());

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(outboxEventService).publish(eq("Payment"), eq(payment.getId()), eq("PaymentCompleted"), any());
    }

    @Test
    void retry_onNonFailedPayment_throwsInvalidPaymentStateException() {
        Payment payment = completedOrFailedPayment("COMPLETED");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.retry(payment.getId()))
                .isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void refund_onCompletedWalletPayment_creditsWalletAndMarksRefunded() {
        UUID walletId = UUID.randomUUID();
        Payment payment = completedOrFailedPayment("COMPLETED");
        payment.setMethod("WALLET");
        payment.setWalletId(walletId);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.refund(payment.getId());

        assertThat(response.getStatus()).isEqualTo("REFUNDED");
        verify(walletService).credit(walletId, payment.getAmount());
        verify(outboxEventService).publish(eq("Payment"), eq(payment.getId()), eq("PaymentRefunded"), any());
    }

    @Test
    void refund_onNonCompletedPayment_throwsInvalidPaymentStateException() {
        Payment payment = completedOrFailedPayment("FAILED");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refund(payment.getId()))
                .isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void getPaymentById_whenMissing_throwsPaymentNotFoundException() {
        UUID id = UUID.randomUUID();
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(id))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    private PaymentCreateRequest cardRequest(BigDecimal amount) {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setInvoiceId(UUID.randomUUID());
        request.setAmount(amount);
        request.setMethod("CARD");
        return request;
    }

    private Payment completedOrFailedPayment(String status) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setInvoiceId(UUID.randomUUID());
        payment.setAmount(new BigDecimal("75.00"));
        payment.setMethod("CARD");
        payment.setStatus(status);
        payment.setIdempotencyKey(UUID.randomUUID().toString());
        payment.setCreatedAt(Instant.now());
        return payment;
    }
}
