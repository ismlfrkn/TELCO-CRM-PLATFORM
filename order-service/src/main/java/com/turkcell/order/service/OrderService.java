package com.turkcell.order.service;

import com.turkcell.order.client.*;
import com.turkcell.order.dto.request.OrderCreateRequest;
import com.turkcell.order.dto.request.OrderItemRequest;
import com.turkcell.order.dto.response.OrderItemResponse;
import com.turkcell.order.dto.response.OrderResponse;
import com.turkcell.order.entity.Order;
import com.turkcell.order.entity.OrderItem;
import com.turkcell.order.exception.CustomerNotFoundException;
import com.turkcell.order.exception.InvalidOrderStateException;
import com.turkcell.order.exception.OrderNotFoundException;
import com.turkcell.order.exception.ProductNotFoundException;
import com.turkcell.order.mapper.OrderMapper;
import com.turkcell.order.repository.OrderItemRepository;
import com.turkcell.order.repository.OrderRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dokuman bolum 9.2'deki saga akisinin uygulamasi. Gercek uretimde 4. ve 5. adimlar (Order -> Payment,
 * Order -> Subscription) Kafka uzerinden ASENKRON calisir (bkz. 9.1 karar tablosu); bu platformda henuz
 * bir Kafka consumer altyapisi kurulmadigi icin (diger tum servislerle ayni bilincli kapsam disi
 * birakma), bu saga SENKRON REST cagrilariyla (OpenFeign) tek bir istek-cevap dongusunde yurutulur.
 * Musteri/urun dogrulama adimlari zaten dokumanin kendisinde de "Senkron" olarak tanimli (9.1).
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String AGGREGATE_TYPE = "Order";
    private static final String PAYMENT_METHOD = "CARD";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPersistenceService orderPersistenceService;
    private final IdempotencyKeyService idempotencyKeyService;
    private final OutboxEventService outboxEventService;
    private final OrderMapper orderMapper;
    private final CustomerServiceClient customerServiceClient;
    private final ProductCatalogServiceClient productCatalogServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final SubscriptionServiceClient subscriptionServiceClient;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                         OrderPersistenceService orderPersistenceService, IdempotencyKeyService idempotencyKeyService,
                         OutboxEventService outboxEventService, OrderMapper orderMapper,
                         CustomerServiceClient customerServiceClient,
                         ProductCatalogServiceClient productCatalogServiceClient,
                         PaymentServiceClient paymentServiceClient,
                         SubscriptionServiceClient subscriptionServiceClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderPersistenceService = orderPersistenceService;
        this.idempotencyKeyService = idempotencyKeyService;
        this.outboxEventService = outboxEventService;
        this.orderMapper = orderMapper;
        this.customerServiceClient = customerServiceClient;
        this.productCatalogServiceClient = productCatalogServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.subscriptionServiceClient = subscriptionServiceClient;
    }

    public OrderResponse createOrder(String idempotencyKey, OrderCreateRequest request) {
        String requestHash = idempotencyKeyService.hash(request);
        Optional<OrderResponse> cached = idempotencyKeyService.tryClaim(idempotencyKey, requestHash, OrderResponse.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        OrderResponse response = executeSaga(idempotencyKey, request);
        idempotencyKeyService.complete(idempotencyKey, response, 201);
        return response;
    }

    private OrderResponse executeSaga(String idempotencyKey, OrderCreateRequest request) {
        validateCustomerExists(request.getCustomerId());
        List<OrderItem> items = resolveItems(request.getItems());
        BigDecimal totalAmount = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = orderPersistenceService.createOrderRecord(request.getCustomerId(), totalAmount, items);
        outboxEventService.publish(AGGREGATE_TYPE, order.getId(), "OrderCreated", toOrderResponse(order, items));

        orderPersistenceService.markAwaitingPayment(order.getId());
        PaymentClientResponse payment = paymentServiceClient.createPayment(
                "order-payment-" + idempotencyKey, paymentRequest(order));

        if (!"COMPLETED".equals(payment.getStatus())) {
            orderPersistenceService.cancelOrder(order.getId());
            outboxEventService.publish(AGGREGATE_TYPE, order.getId(), "OrderCancelled", toOrderResponse(order, items));
            return toOrderResponse(order, items);
        }
        orderPersistenceService.markOrderPaid(order.getId());

        Optional<OrderItem> tariffItem = items.stream()
                .filter(item -> "TARIFF".equals(item.getProductType()))
                .findFirst();

        if (tariffItem.isPresent()) {
            try {
                subscriptionServiceClient.createSubscription(
                        subscriptionRequest(request.getCustomerId(), tariffItem.get().getProductCode()));
            } catch (Exception ex) {
                log.warn("Subscription activation failed for order {}, triggering payment refund compensation",
                        order.getId(), ex);
                paymentServiceClient.refund(payment.getId());
                orderPersistenceService.cancelOrder(order.getId());
                outboxEventService.publish(AGGREGATE_TYPE, order.getId(), "OrderCancelled", toOrderResponse(order, items));
                return toOrderResponse(order, items);
            }
        }

        orderPersistenceService.markOrderFulfilled(order.getId());
        outboxEventService.publish(AGGREGATE_TYPE, order.getId(), "OrderConfirmed", toOrderResponse(order, items));
        return toOrderResponse(order, items);
    }

    public OrderResponse getOrderResponseById(UUID id) {
        return toOrderResponse(getOrderById(id));
    }

    /**
     * Musteri/CSR tarafindan manuel tetiklenen iptal (FR-12) - saga basarisizliginda otomatik
     * tetiklenen kompansasyondan (executeSaga icindeki cancelOrder cagrilari) farkli bir yol.
     * Zaten tamamlanmis (FULFILLED) ya da iptal edilmis siparisler tekrar iptal edilemez.
     */
    public OrderResponse cancelOrder(UUID id) {
        Order order = getOrderById(id);
        if (Order.STATUS_FULFILLED.equals(order.getStatus()) || Order.STATUS_CANCELLED.equals(order.getStatus())) {
            throw new InvalidOrderStateException(
                    "Cannot cancel order in status " + order.getStatus() + ", id: " + id);
        }

        orderPersistenceService.cancelOrder(id);
        OrderResponse response = toOrderResponse(order);
        outboxEventService.publish(AGGREGATE_TYPE, id, "OrderCancelled", response);
        return response;
    }

    public Order getOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
    }

    private void validateCustomerExists(UUID customerId) {
        try {
            customerServiceClient.getCustomer(customerId);
        } catch (FeignException.NotFound ex) {
            throw new CustomerNotFoundException("Customer not found with id: " + customerId);
        }
    }

    private List<OrderItem> resolveItems(List<OrderItemRequest> requestItems) {
        List<OrderItem> resolved = new ArrayList<>();
        for (OrderItemRequest requestItem : requestItems) {
            BigDecimal unitPrice = switch (requestItem.getProductType()) {
                case "TARIFF" -> resolveTariffPrice(requestItem.getProductCode());
                case "ADDON" -> resolveAddonPrice(requestItem.getProductCode());
                default -> throw new ProductNotFoundException("Unsupported product type: " + requestItem.getProductType());
            };

            OrderItem item = new OrderItem();
            item.setProductCode(requestItem.getProductCode());
            item.setProductType(requestItem.getProductType());
            item.setQuantity(requestItem.getQuantity());
            item.setUnitPrice(unitPrice);
            resolved.add(item);
        }
        return resolved;
    }

    private BigDecimal resolveTariffPrice(String code) {
        try {
            return productCatalogServiceClient.getTariff(code).getMonthlyFee();
        } catch (FeignException.NotFound ex) {
            throw new ProductNotFoundException("Tariff not found with code: " + code);
        }
    }

    private BigDecimal resolveAddonPrice(String code) {
        try {
            return productCatalogServiceClient.getAddon(code).getPrice();
        } catch (FeignException.NotFound ex) {
            throw new ProductNotFoundException("Addon not found with code: " + code);
        }
    }

    private PaymentClientRequest paymentRequest(Order order) {
        PaymentClientRequest request = new PaymentClientRequest();
        request.setAmount(order.getTotalAmount());
        request.setCurrency(order.getCurrency());
        request.setMethod(PAYMENT_METHOD);
        return request;
    }

    private SubscriptionClientRequest subscriptionRequest(UUID customerId, String tariffCode) {
        SubscriptionClientRequest request = new SubscriptionClientRequest();
        request.setCustomerId(customerId);
        request.setTariffCode(tariffCode);
        return request;
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());
        return toOrderResponse(order, items);
    }

    private OrderResponse toOrderResponse(Order order, List<OrderItem> items) {
        OrderResponse response = orderMapper.toResponse(order);
        response.setItems(items.stream().map(this::toItemResponse).collect(Collectors.toList()));
        return response;
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setProductCode(item.getProductCode());
        response.setProductType(item.getProductType());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        return response;
    }
}
