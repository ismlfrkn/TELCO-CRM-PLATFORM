package com.turkcell.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.order.entity.Order;
import com.turkcell.order.entity.OrderItem;
import com.turkcell.order.mapper.OrderMapper;
import com.turkcell.order.repository.OrderItemRepository;
import com.turkcell.order.repository.OrderRepository;
import com.turkcell.order.service.OrderPersistenceService;
import com.turkcell.order.service.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SagaEventConsumerConfigTest {

    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private OrderPersistenceService orderPersistenceService;
    private OutboxEventService outboxEventService;
    private SagaEventConsumerConfig config;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        orderPersistenceService = mock(OrderPersistenceService.class);
        outboxEventService = mock(OutboxEventService.class);
        OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

        config = new SagaEventConsumerConfig(orderRepository, orderItemRepository, orderPersistenceService,
                outboxEventService, orderMapper, new ObjectMapper());
    }

    private Order order(UUID id, String status) {
        Order order = new Order();
        order.setId(id);
        order.setCustomerId(UUID.randomUUID());
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setCurrency("TRY");
        order.setCreatedAt(Instant.now());
        return order;
    }

    private OrderItem item(UUID orderId, String productType) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductCode("CODE-1");
        item.setProductType(productType);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("100.00"));
        return item;
    }

    private String envelope(String eventType, UUID orderId) {
        return """
                {"eventId":"%s","aggregateType":"X","aggregateId":"%s","eventType":"%s","occurredAt":"2026-07-11T00:00:00Z",
                "payload":{"orderId":"%s"}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), eventType, orderId);
    }

    @Test
    void paymentCompletedWithTariffItem_marksOrderPaidButNotYetFulfilled() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order(orderId, Order.STATUS_PENDING_PAYMENT)));
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of(item(orderId, "TARIFF")));

        Consumer<Message<String>> consumer = config.paymentEvents();
        consumer.accept(MessageBuilder.withPayload(envelope("PaymentCompleted", orderId)).build());

        verify(orderPersistenceService).markOrderPaid(orderId);
        verify(orderPersistenceService, never()).markOrderFulfilled(any());
        verify(outboxEventService, never()).publish(anyString(), any(), eq("OrderConfirmed"), any());
    }

    @Test
    void paymentCompletedWithOnlyAddonItems_fulfillsOrderImmediately() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order(orderId, Order.STATUS_PENDING_PAYMENT)));
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of(item(orderId, "ADDON")));

        Consumer<Message<String>> consumer = config.paymentEvents();
        consumer.accept(MessageBuilder.withPayload(envelope("PaymentCompleted", orderId)).build());

        verify(orderPersistenceService).markOrderPaid(orderId);
        verify(orderPersistenceService).markOrderFulfilled(orderId);
        verify(outboxEventService).publish(eq("Order"), eq(orderId), eq("OrderConfirmed"), any());
    }

    @Test
    void paymentCompletedForOrderNotInPendingPayment_isIgnored() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order(orderId, Order.STATUS_PAID)));

        config.paymentEvents().accept(MessageBuilder.withPayload(envelope("PaymentCompleted", orderId)).build());

        verify(orderPersistenceService, never()).markOrderPaid(any());
    }

    @Test
    void paymentFailedCancelsOrder() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order(orderId, Order.STATUS_PENDING_PAYMENT)));
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of());

        config.paymentEvents().accept(MessageBuilder.withPayload(envelope("PaymentFailed", orderId)).build());

        verify(orderPersistenceService).cancelOrder(orderId);
        verify(outboxEventService).publish(eq("Order"), eq(orderId), eq("OrderCancelled"), any());
    }

    @Test
    void subscriptionActivatedFulfillsOrder() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order(orderId, Order.STATUS_PAID)));
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of());

        config.subscriptionEvents().accept(MessageBuilder.withPayload(envelope("SubscriptionActivated", orderId)).build());

        verify(orderPersistenceService).markOrderFulfilled(orderId);
        verify(outboxEventService).publish(eq("Order"), eq(orderId), eq("OrderConfirmed"), any());
    }

    @Test
    void subscriptionActivationFailedCancelsOrderAsCompensation() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order(orderId, Order.STATUS_PAID)));
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of());

        config.subscriptionEvents().accept(
                MessageBuilder.withPayload(envelope("SubscriptionActivationFailed", orderId)).build());

        verify(orderPersistenceService).cancelOrder(orderId);
        verify(outboxEventService).publish(eq("Order"), eq(orderId), eq("OrderCancelled"), any());
    }

    @Test
    void subscriptionActivatedForOrderNotPaid_isIgnored() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order(orderId, Order.STATUS_FULFILLED)));

        config.subscriptionEvents().accept(MessageBuilder.withPayload(envelope("SubscriptionActivated", orderId)).build());

        verify(orderPersistenceService, never()).markOrderFulfilled(any());
    }

    @Test
    void missingOrderIdInPayloadIsIgnored() {
        String json = """
                {"eventId":"%s","aggregateType":"Payment","aggregateId":"%s","eventType":"PaymentCompleted","occurredAt":"2026-07-11T00:00:00Z","payload":{}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        config.paymentEvents().accept(MessageBuilder.withPayload(json).build());

        verifyNoInteractions(orderPersistenceService);
    }

    @Test
    void malformedEnvelopeIsIgnoredWithoutThrowing() {
        config.paymentEvents().accept(MessageBuilder.withPayload("not-json").build());

        verifyNoInteractions(orderPersistenceService);
    }
}
