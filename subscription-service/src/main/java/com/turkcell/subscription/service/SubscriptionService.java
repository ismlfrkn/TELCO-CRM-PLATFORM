package com.turkcell.subscription.service;

import com.turkcell.subscription.dto.request.SubscriptionCreateRequest;
import com.turkcell.subscription.dto.response.SubscriptionResponse;
import com.turkcell.subscription.entity.Subscription;
import com.turkcell.subscription.exception.InvalidSubscriptionTransitionException;
import com.turkcell.subscription.exception.SubscriptionNotFoundException;
import com.turkcell.subscription.mapper.SubscriptionMapper;
import com.turkcell.subscription.repository.SubscriptionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * musteri/tarife dogrulugu bu servisin sorumlulugunda degildir: dokumanin 9.1 bolumune gore bu
 * kontroller Order Service tarafinda (senkron) yapilir, Order Service subscription'i "zaten
 * dogrulanmis" olarak olusturur/yonetir. order-service henuz yazilmadigi icin bu endpoint'ler su an
 * dogrudan cagrilabilir haldedir (identity/customer/catalog servisleriyle ayni test edilebilirlik
 * seviyesinde).
 */
@Service
public class SubscriptionService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String STATUS_TERMINATED = "TERMINATED";

    private static final String AGGREGATE_TYPE = "Subscription";
    private static final String EVENT_ACTIVATED = "SubscriptionActivated";
    private static final String EVENT_SUSPENDED = "SubscriptionSuspended";
    private static final String EVENT_TERMINATED = "SubscriptionTerminated";

    private final SubscriptionRepository subscriptionRepository;
    private final MsisdnPoolService msisdnPoolService;
    private final SubscriptionMapper subscriptionMapper;
    private final OutboxEventService outboxEventService;
    private final AuditLogService auditLogService;
    private final TransactionTemplate newSubscriptionTransactionTemplate;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, MsisdnPoolService msisdnPoolService,
                                SubscriptionMapper subscriptionMapper, OutboxEventService outboxEventService,
                                AuditLogService auditLogService, PlatformTransactionManager transactionManager) {
        this.subscriptionRepository = subscriptionRepository;
        this.msisdnPoolService = msisdnPoolService;
        this.subscriptionMapper = subscriptionMapper;
        this.outboxEventService = outboxEventService;
        this.auditLogService = auditLogService;

        // REQUIRES_NEW: MSISDN tahsisi + Subscription insert'i TEK bir izole transaction'da yapilir -
        // order_id UNIQUE kisitina carpip rollback olursa, tahsis edilen MSISDN de ayni transaction
        // icinde geri alinir (sizinti olmaz). payment-service'teki createPayment ile ayni desen.
        this.newSubscriptionTransactionTemplate = new TransactionTemplate(transactionManager);
        this.newSubscriptionTransactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * request.orderId doluysa (PaymentCompleted event'i uzerinden tetiklenen aktivasyon), ayni siparis
     * icin ikinci kez cagrilirsa (en-az-bir-kez teslim) yeni bir abonelik acmaz, ilk sonucu dondurur.
     * orderId bossa (dogrudan REST cagrisi) idempotency kontrolu atlanir, eski davranis aynen sürer.
     */
    public SubscriptionResponse createSubscription(SubscriptionCreateRequest request) {
        if (request.getOrderId() != null) {
            Optional<Subscription> existing = subscriptionRepository.findByOrderId(request.getOrderId());
            if (existing.isPresent()) {
                return subscriptionMapper.toResponse(existing.get());
            }
        }

        try {
            return newSubscriptionTransactionTemplate.execute(status -> processNewSubscription(request));
        } catch (DataIntegrityViolationException ex) {
            if (request.getOrderId() != null) {
                return subscriptionRepository.findByOrderId(request.getOrderId())
                        .map(subscriptionMapper::toResponse)
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
    }

    private SubscriptionResponse processNewSubscription(SubscriptionCreateRequest request) {
        String msisdn = (request.getMsisdn() != null && !request.getMsisdn().isBlank())
                ? msisdnPoolService.allocateSpecific(request.getMsisdn())
                : msisdnPoolService.allocateNext();

        Subscription subscription = new Subscription();
        subscription.setCustomerId(request.getCustomerId());
        subscription.setMsisdn(msisdn);
        subscription.setTariffCode(request.getTariffCode());
        subscription.setTariffVersion(request.getTariffVersion());
        subscription.setOrderId(request.getOrderId());
        subscription.setStatus(STATUS_ACTIVE);
        subscription.setActivatedAt(Instant.now());
        subscription = subscriptionRepository.save(subscription);

        SubscriptionResponse response = subscriptionMapper.toResponse(subscription);
        outboxEventService.publish(AGGREGATE_TYPE, subscription.getId(), EVENT_ACTIVATED, response);
        auditLogService.record("SUBSCRIPTION_ACTIVATED", AGGREGATE_TYPE, subscription.getId(), null, response);
        return response;
    }

    public SubscriptionResponse getSubscriptionResponseById(UUID id) {
        return subscriptionMapper.toResponse(getSubscriptionById(id));
    }

    public Subscription getSubscriptionById(UUID id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with id: " + id));
    }

    public Page<SubscriptionResponse> getSubscriptionsByCustomer(UUID customerId, Pageable pageable) {
        return subscriptionRepository.findAllByCustomerId(customerId, pageable).map(subscriptionMapper::toResponse);
    }

    /**
     * FR-21: billing-service'in bill-run'i otomatik tetikleyebilmesi icin - "hangi aboneler
     * faturalanacak" bilgisini artik cagiran taraf elle vermek yerine, billing-service bu endpoint'i
     * senkron cagirip aktif abone listesini kendisi cekiyor (bkz. billing-service BillingRunService).
     */
    public Page<SubscriptionResponse> getActiveSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAllByStatus(STATUS_ACTIVE, pageable).map(subscriptionMapper::toResponse);
    }

    @Transactional
    public SubscriptionResponse suspend(UUID id) {
        Subscription subscription = getSubscriptionById(id);
        requireStatus(subscription, STATUS_ACTIVE, "suspend");

        SubscriptionResponse before = subscriptionMapper.toResponse(subscription);
        subscription.setStatus(STATUS_SUSPENDED);
        subscriptionRepository.save(subscription);

        SubscriptionResponse after = subscriptionMapper.toResponse(subscription);
        outboxEventService.publish(AGGREGATE_TYPE, subscription.getId(), EVENT_SUSPENDED, after);
        auditLogService.record("SUBSCRIPTION_SUSPENDED", AGGREGATE_TYPE, subscription.getId(), before, after);
        return after;
    }

    @Transactional
    public SubscriptionResponse reactivate(UUID id) {
        Subscription subscription = getSubscriptionById(id);
        requireStatus(subscription, STATUS_SUSPENDED, "reactivate");

        SubscriptionResponse before = subscriptionMapper.toResponse(subscription);
        subscription.setStatus(STATUS_ACTIVE);
        subscriptionRepository.save(subscription);

        // Dokumanin event sozlugunde ayri bir "SubscriptionReactivated" event'i tanimli degil;
        // tuketiciler acisindan "tekrar aktif" sinyali SubscriptionActivated ile ayni anlama gelir.
        SubscriptionResponse after = subscriptionMapper.toResponse(subscription);
        outboxEventService.publish(AGGREGATE_TYPE, subscription.getId(), EVENT_ACTIVATED, after);
        auditLogService.record("SUBSCRIPTION_REACTIVATED", AGGREGATE_TYPE, subscription.getId(), before, after);
        return after;
    }

    @Transactional
    public SubscriptionResponse terminate(UUID id) {
        Subscription subscription = getSubscriptionById(id);
        if (STATUS_TERMINATED.equals(subscription.getStatus())) {
            throw new InvalidSubscriptionTransitionException(
                    "Subscription is already TERMINATED, id: " + id);
        }

        SubscriptionResponse before = subscriptionMapper.toResponse(subscription);
        subscription.setStatus(STATUS_TERMINATED);
        subscription.setTerminatedAt(Instant.now());
        subscriptionRepository.save(subscription);
        msisdnPoolService.release(subscription.getMsisdn());

        SubscriptionResponse after = subscriptionMapper.toResponse(subscription);
        outboxEventService.publish(AGGREGATE_TYPE, subscription.getId(), EVENT_TERMINATED, after);
        auditLogService.record("SUBSCRIPTION_TERMINATED", AGGREGATE_TYPE, subscription.getId(), before, after);
        return after;
    }

    private void requireStatus(Subscription subscription, String requiredStatus, String operation) {
        if (!requiredStatus.equals(subscription.getStatus())) {
            throw new InvalidSubscriptionTransitionException(
                    "Cannot " + operation + " subscription: requires status " + requiredStatus
                            + ", current status: " + subscription.getStatus());
        }
    }
}
