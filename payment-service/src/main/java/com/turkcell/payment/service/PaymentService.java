package com.turkcell.payment.service;

import com.turkcell.payment.dto.request.PaymentCreateRequest;
import com.turkcell.payment.dto.response.PaymentResponse;
import com.turkcell.payment.entity.Payment;
import com.turkcell.payment.entity.PaymentAttempt;
import com.turkcell.payment.exception.InvalidPaymentStateException;
import com.turkcell.payment.exception.InsufficientWalletBalanceException;
import com.turkcell.payment.exception.MissingWalletIdException;
import com.turkcell.payment.exception.PaymentNotFoundException;
import com.turkcell.payment.mapper.PaymentMapper;
import com.turkcell.payment.repository.PaymentAttemptRepository;
import com.turkcell.payment.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

/**
 * musteri/fatura dogrulugu bu servisin sorumlulugunda degildir: subscription-service ile ayni
 * gerekceyle, bu kontrollerin cagiran (Order/Billing Service) tarafinda yapildigi varsayilir.
 */
@Service
public class PaymentService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_REFUNDED = "REFUNDED";
    private static final String METHOD_WALLET = "WALLET";

    private static final String AGGREGATE_TYPE = "Payment";

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final WalletService walletService;
    private final PaymentMapper paymentMapper;
    private final OutboxEventService outboxEventService;
    private final AuditLogService auditLogService;
    private final TransactionTemplate newPaymentTransactionTemplate;

    public PaymentService(PaymentRepository paymentRepository, PaymentAttemptRepository paymentAttemptRepository,
                           WalletService walletService, PaymentMapper paymentMapper,
                           OutboxEventService outboxEventService, AuditLogService auditLogService,
                           PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.walletService = walletService;
        this.paymentMapper = paymentMapper;
        this.outboxEventService = outboxEventService;
        this.auditLogService = auditLogService;

        // REQUIRES_NEW: bilerek AYRI bir transaction'da calisir. Postgres bir statement'ta hata
        // verdiginde tum transaction'i "aborted" isaretler, sonraki komutlar (fallback SELECT dahil)
        // ayni transaction icinde calismaz. Bu yuzden riskli insert kendi izole transaction'inda
        // yapilir: basarisiz olursa SADECE o kucuk transaction rollback olur, disaridaki cagri
        // (createPayment) DataIntegrityViolationException'i temiz bir transaction baglaminda yakalayip
        // fallback SELECT'i calistirabilir.
        this.newPaymentTransactionTemplate = new TransactionTemplate(transactionManager);
        this.newPaymentTransactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * FR-26: ayni idempotencyKey (Idempotency-Key header) ile iki kez cagrilirsa odeme tekrar
     * islenmez, ilk cagrinin sonucu aynen dondurulur. Once-kontrol-sonra-yaz araligindaki yarisi
     * kaybeden eszamanli istek icin DB'deki UNIQUE constraint son güvenlik agidir.
     */
    public PaymentResponse createPayment(String idempotencyKey, PaymentCreateRequest request) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .map(paymentMapper::toResponse)
                .orElseGet(() -> {
                    try {
                        return newPaymentTransactionTemplate.execute(
                                status -> processNewPayment(idempotencyKey, request));
                    } catch (DataIntegrityViolationException ex) {
                        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                                .map(paymentMapper::toResponse)
                                .orElseThrow(() -> ex);
                    }
                });
    }

    private PaymentResponse processNewPayment(String idempotencyKey, PaymentCreateRequest request) {
        if (METHOD_WALLET.equals(request.getMethod()) && request.getWalletId() == null) {
            throw new MissingWalletIdException("walletId is required when method is WALLET");
        }

        Payment payment = new Payment();
        payment.setIdempotencyKey(idempotencyKey);
        payment.setInvoiceId(request.getInvoiceId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setMethod(request.getMethod());
        payment.setWalletId(request.getWalletId());
        payment.setStatus(STATUS_PENDING);
        payment = paymentRepository.save(payment);

        MockPspResult result = charge(payment, request.isSimulateFailure());
        recordAttempt(payment.getId(), 1, result);

        if (result.success()) {
            payment.setStatus(STATUS_COMPLETED);
            payment.setExternalRef(result.externalRef());
            payment.setPaidAt(Instant.now());
        } else {
            payment.setStatus(STATUS_FAILED);
        }
        paymentRepository.save(payment);

        PaymentResponse response = paymentMapper.toResponse(payment);
        String eventType = result.success() ? "PaymentCompleted" : "PaymentFailed";
        String action = result.success() ? "PAYMENT_COMPLETED" : "PAYMENT_FAILED";
        outboxEventService.publish(AGGREGATE_TYPE, payment.getId(), eventType, response);
        auditLogService.record(action, AGGREGATE_TYPE, payment.getId(), null, response);
        return response;
    }

    public PaymentResponse getPaymentResponseById(UUID id) {
        return paymentMapper.toResponse(getPaymentById(id));
    }

    public Payment getPaymentById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));
    }

    @Transactional
    public PaymentResponse retry(UUID id) {
        Payment payment = getPaymentById(id);
        if (!STATUS_FAILED.equals(payment.getStatus())) {
            throw new InvalidPaymentStateException(
                    "Only FAILED payments can be retried, current status: " + payment.getStatus());
        }

        int nextAttemptNo = paymentAttemptRepository.countByPaymentId(id) + 1;
        MockPspResult result = charge(payment, false);
        recordAttempt(id, nextAttemptNo, result);

        if (result.success()) {
            payment.setStatus(STATUS_COMPLETED);
            payment.setExternalRef(result.externalRef());
            payment.setPaidAt(Instant.now());
        }
        paymentRepository.save(payment);

        PaymentResponse response = paymentMapper.toResponse(payment);
        if (result.success()) {
            outboxEventService.publish(AGGREGATE_TYPE, payment.getId(), "PaymentCompleted", response);
            auditLogService.record("PAYMENT_RETRY_SUCCEEDED", AGGREGATE_TYPE, payment.getId(), null, response);
        } else {
            auditLogService.record("PAYMENT_RETRY_FAILED", AGGREGATE_TYPE, payment.getId(), null, response);
        }
        return response;
    }

    @Transactional
    public PaymentResponse refund(UUID id) {
        Payment payment = getPaymentById(id);
        if (!STATUS_COMPLETED.equals(payment.getStatus())) {
            throw new InvalidPaymentStateException(
                    "Only COMPLETED payments can be refunded, current status: " + payment.getStatus());
        }

        if (METHOD_WALLET.equals(payment.getMethod())) {
            walletService.credit(payment.getWalletId(), payment.getAmount());
        }

        payment.setStatus(STATUS_REFUNDED);
        paymentRepository.save(payment);

        PaymentResponse response = paymentMapper.toResponse(payment);
        outboxEventService.publish(AGGREGATE_TYPE, payment.getId(), "PaymentRefunded", response);
        auditLogService.record("PAYMENT_REFUNDED", AGGREGATE_TYPE, payment.getId(), null, response);
        return response;
    }

    private MockPspResult charge(Payment payment, boolean simulateFailure) {
        if (simulateFailure) {
            return MockPspResult.failure("Mock PSP declined the transaction");
        }
        if (METHOD_WALLET.equals(payment.getMethod())) {
            try {
                walletService.debit(payment.getWalletId(), payment.getAmount());
            } catch (InsufficientWalletBalanceException ex) {
                return MockPspResult.failure(ex.getMessage());
            }
        }
        return MockPspResult.success("MOCK-" + UUID.randomUUID());
    }

    private void recordAttempt(UUID paymentId, int attemptNo, MockPspResult result) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setPaymentId(paymentId);
        attempt.setAttemptNo(attemptNo);
        attempt.setResponse(result.success()
                ? "{\"success\":true,\"externalRef\":\"" + result.externalRef() + "\"}"
                : "{\"success\":false,\"message\":\"" + result.message() + "\"}");
        paymentAttemptRepository.save(attempt);
    }
}
