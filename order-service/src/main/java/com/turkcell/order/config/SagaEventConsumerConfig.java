package com.turkcell.order.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.order.dto.response.OrderItemResponse;
import com.turkcell.order.dto.response.OrderResponse;
import com.turkcell.order.entity.Order;
import com.turkcell.order.entity.OrderItem;
import com.turkcell.order.mapper.OrderMapper;
import com.turkcell.order.repository.OrderItemRepository;
import com.turkcell.order.repository.OrderRepository;
import com.turkcell.order.service.OrderPersistenceService;
import com.turkcell.order.service.OutboxEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Configuration
public class SagaEventConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(SagaEventConsumerConfig.class);
    private static final String AGGREGATE_TYPE = "Order";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPersistenceService orderPersistenceService;
    private final OutboxEventService outboxEventService;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    public SagaEventConsumerConfig(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                                    OrderPersistenceService orderPersistenceService,
                                    OutboxEventService outboxEventService, OrderMapper orderMapper,
                                    ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderPersistenceService = orderPersistenceService;
        this.outboxEventService = outboxEventService;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Consumer<Message<String>> paymentEvents() {
        return message -> {
            JsonNode envelope = readEnvelope(message, "telco.payment.events");
            if (envelope == null) {
                return;
            }
            String eventType = envelope.get("eventType").asText();
            if (!"PaymentCompleted".equals(eventType) && !"PaymentFailed".equals(eventType)) {
                return;
            }

            UUID orderId = extractOrderId(envelope);
            if (orderId == null) {
                return;
            }

            Order order = requireOrderInStatus(orderId, Order.STATUS_PENDING_PAYMENT, eventType);
            if (order == null) {
                return;
            }

            if ("PaymentCompleted".equals(eventType)) {
                handlePaymentCompleted(orderId);
            } else {
                orderPersistenceService.cancelOrder(orderId);
                publishOrderEvent(orderId, "OrderCancelled");
            }
        };
    }

    @Bean
    public Consumer<Message<String>> subscriptionEvents() {
        return message -> {
            JsonNode envelope = readEnvelope(message, "telco.subscription.events");
            if (envelope == null) {
                return;
            }
            String eventType = envelope.get("eventType").asText();
            if (!"SubscriptionActivated".equals(eventType) && !"SubscriptionActivationFailed".equals(eventType)) {
                return;
            }

            UUID orderId = extractOrderId(envelope);
            if (orderId == null) {
                return;
            }

            Order order = requireOrderInStatus(orderId, Order.STATUS_PAID, eventType);
            if (order == null) {
                return;
            }

            if ("SubscriptionActivated".equals(eventType)) {
                orderPersistenceService.markOrderFulfilled(orderId);
                publishOrderEvent(orderId, "OrderConfirmed");
            } else {
                orderPersistenceService.cancelOrder(orderId);
                publishOrderEvent(orderId, "OrderCancelled");
            }
        };
    }

    private void handlePaymentCompleted(UUID orderId) {
        orderPersistenceService.markOrderPaid(orderId);

        List<OrderItem> items = orderItemRepository.findAllByOrderId(orderId);
        boolean hasTariffItem = items.stream().anyMatch(item -> "TARIFF".equals(item.getProductType()));
        if (!hasTariffItem) {

            orderPersistenceService.markOrderFulfilled(orderId);
            publishOrderEvent(orderId, "OrderConfirmed");
        }
    }

    private Order requireOrderInStatus(UUID orderId, String expectedStatus, String eventType) {
        Optional<Order> maybeOrder = orderRepository.findById(orderId);
        if (maybeOrder.isEmpty()) {
            log.warn("Received {} for unknown order {}", eventType, orderId);
            return null;
        }
        Order order = maybeOrder.get();
        if (!expectedStatus.equals(order.getStatus())) {
            log.info("Order {} is not in expected status {} (current={}), ignoring {} (muhtemelen yinelenen teslim)",
                    orderId, expectedStatus, order.getStatus(), eventType);
            return null;
        }
        return order;
    }

    private void publishOrderEvent(UUID orderId, String eventType) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order disappeared mid-saga: " + orderId));
        List<OrderItem> items = orderItemRepository.findAllByOrderId(orderId);

        OrderResponse response = orderMapper.toResponse(order);
        response.setItems(items.stream().map(this::toItemResponse).collect(Collectors.toList()));
        outboxEventService.publish(AGGREGATE_TYPE, orderId, eventType, response);
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setProductCode(item.getProductCode());
        response.setProductType(item.getProductType());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        return response;
    }

    private UUID extractOrderId(JsonNode envelope) {
        JsonNode payload = envelope.get("payload");
        if (payload == null || !payload.hasNonNull("orderId")) {
            return null;
        }
        return UUID.fromString(payload.get("orderId").asText());
    }

    private JsonNode readEnvelope(Message<String> message, String sourceTopic) {
        try {
            return objectMapper.readTree(message.getPayload());
        } catch (Exception ex) {
            log.error("Malformed event envelope from {}: {}", sourceTopic, message.getPayload(), ex);
            return null;
        }
    }
}
