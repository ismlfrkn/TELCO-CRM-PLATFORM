package com.turkcell.order.service;

import com.turkcell.order.client.AddonClientDto;
import com.turkcell.order.client.CustomerClientDto;
import com.turkcell.order.client.PaymentClientResponse;
import com.turkcell.order.client.SubscriptionClientResponse;
import com.turkcell.order.client.TariffClientDto;
import com.turkcell.order.client.gateway.CustomerServiceGateway;
import com.turkcell.order.client.gateway.PaymentServiceGateway;
import com.turkcell.order.client.gateway.ProductCatalogServiceGateway;
import com.turkcell.order.client.gateway.SubscriptionServiceGateway;
import com.turkcell.order.dto.request.OrderCreateRequest;
import com.turkcell.order.dto.request.OrderItemRequest;
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
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private OrderPersistenceService orderPersistenceService;
    private IdempotencyKeyService idempotencyKeyService;
    private OutboxEventService outboxEventService;
    private CustomerServiceGateway customerServiceGateway;
    private ProductCatalogServiceGateway productCatalogServiceGateway;
    private PaymentServiceGateway paymentServiceGateway;
    private SubscriptionServiceGateway subscriptionServiceGateway;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        orderPersistenceService = mock(OrderPersistenceService.class);
        idempotencyKeyService = mock(IdempotencyKeyService.class);
        outboxEventService = mock(OutboxEventService.class);
        customerServiceGateway = mock(CustomerServiceGateway.class);
        productCatalogServiceGateway = mock(ProductCatalogServiceGateway.class);
        paymentServiceGateway = mock(PaymentServiceGateway.class);
        subscriptionServiceGateway = mock(SubscriptionServiceGateway.class);
        OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

        orderService = new OrderService(orderRepository, orderItemRepository, orderPersistenceService,
                idempotencyKeyService, outboxEventService, orderMapper, customerServiceGateway,
                productCatalogServiceGateway, paymentServiceGateway, subscriptionServiceGateway);

        when(idempotencyKeyService.hash(any())).thenReturn("request-hash");
        when(idempotencyKeyService.tryClaim(any(), any(), eq(OrderResponse.class))).thenReturn(Optional.empty());

        CustomerClientDto customer = new CustomerClientDto();
        customer.setStatus("ACTIVE");
        when(customerServiceGateway.getCustomer(any())).thenReturn(customer);
    }

    @Test
    void createOrder_happyPath_tariffPaymentAndSubscriptionSucceed_fulfillsOrder() {
        UUID customerId = UUID.randomUUID();
        OrderCreateRequest request = requestWithTariff(customerId, "TARIFF100");
        Order order = existingOrder(customerId, new BigDecimal("100.00"));

        TariffClientDto tariff = new TariffClientDto();
        tariff.setCode("TARIFF100");
        tariff.setMonthlyFee(new BigDecimal("100.00"));
        when(productCatalogServiceGateway.getTariff("TARIFF100")).thenReturn(tariff);

        when(orderPersistenceService.createOrderRecord(eq(customerId), eq(new BigDecimal("100.00")), any()))
                .thenReturn(order);

        PaymentClientResponse payment = new PaymentClientResponse();
        payment.setId(UUID.randomUUID());
        payment.setStatus("COMPLETED");
        when(paymentServiceGateway.createPayment(any(), any())).thenReturn(payment);

        when(subscriptionServiceGateway.createSubscription(any())).thenReturn(new SubscriptionClientResponse());

        OrderResponse response = orderService.createOrder("idem-key-1", request);

        assertThat(response.getId()).isEqualTo(order.getId());
        verify(orderPersistenceService).markOrderFulfilled(order.getId());
        verify(orderPersistenceService, never()).cancelOrder(any());
        verify(subscriptionServiceGateway).createSubscription(any());
        verify(outboxEventService).publish(eq("Order"), eq(order.getId()), eq("OrderCreated"), any());
        verify(outboxEventService).publish(eq("Order"), eq(order.getId()), eq("OrderConfirmed"), any());
        verify(idempotencyKeyService).complete(eq("idem-key-1"), any(), eq(201));
    }

    @Test
    void createOrder_whenPaymentFails_cancelsOrderWithoutActivatingSubscription() {
        UUID customerId = UUID.randomUUID();
        OrderCreateRequest request = requestWithAddon(customerId, "ADDON5GB");
        Order order = existingOrder(customerId, new BigDecimal("50.00"));

        AddonClientDto addon = new AddonClientDto();
        addon.setCode("ADDON5GB");
        addon.setPrice(new BigDecimal("50.00"));
        when(productCatalogServiceGateway.getAddon("ADDON5GB")).thenReturn(addon);

        when(orderPersistenceService.createOrderRecord(eq(customerId), eq(new BigDecimal("50.00")), any()))
                .thenReturn(order);

        PaymentClientResponse payment = new PaymentClientResponse();
        payment.setId(UUID.randomUUID());
        payment.setStatus("FAILED");
        when(paymentServiceGateway.createPayment(any(), any())).thenReturn(payment);

        orderService.createOrder("idem-key-2", request);

        verify(orderPersistenceService).cancelOrder(order.getId());
        verify(outboxEventService).publish(eq("Order"), eq(order.getId()), eq("OrderCancelled"), any());
        verifyNoInteractions(subscriptionServiceGateway);
        verify(orderPersistenceService, never()).markOrderFulfilled(any());
    }

    @Test
    void createOrder_whenSubscriptionActivationFails_refundsPaymentAndCancelsOrder() {
        UUID customerId = UUID.randomUUID();
        OrderCreateRequest request = requestWithTariff(customerId, "TARIFF100");
        Order order = existingOrder(customerId, new BigDecimal("100.00"));

        TariffClientDto tariff = new TariffClientDto();
        tariff.setCode("TARIFF100");
        tariff.setMonthlyFee(new BigDecimal("100.00"));
        when(productCatalogServiceGateway.getTariff("TARIFF100")).thenReturn(tariff);

        when(orderPersistenceService.createOrderRecord(eq(customerId), eq(new BigDecimal("100.00")), any()))
                .thenReturn(order);

        UUID paymentId = UUID.randomUUID();
        PaymentClientResponse payment = new PaymentClientResponse();
        payment.setId(paymentId);
        payment.setStatus("COMPLETED");
        when(paymentServiceGateway.createPayment(any(), any())).thenReturn(payment);
        when(subscriptionServiceGateway.createSubscription(any())).thenThrow(new RuntimeException("subscription down"));

        orderService.createOrder("idem-key-3", request);

        verify(paymentServiceGateway).refund(paymentId);
        verify(orderPersistenceService).cancelOrder(order.getId());
        verify(outboxEventService).publish(eq("Order"), eq(order.getId()), eq("OrderCancelled"), any());
        verify(orderPersistenceService, never()).markOrderFulfilled(any());
    }

    @Test
    void createOrder_whenCustomerNotFound_throwsBeforeTouchingPersistenceOrPayment() {
        when(customerServiceGateway.getCustomer(any())).thenThrow(notFound());
        OrderCreateRequest request = requestWithTariff(UUID.randomUUID(), "TARIFF100");

        assertThatThrownBy(() -> orderService.createOrder("idem-key-4", request))
                .isInstanceOf(CustomerNotFoundException.class);

        verifyNoInteractions(orderPersistenceService, paymentServiceGateway, subscriptionServiceGateway);
    }

    @Test
    void createOrder_whenTariffNotFound_throwsProductNotFoundException() {
        when(productCatalogServiceGateway.getTariff("MISSING")).thenThrow(notFound());
        OrderCreateRequest request = requestWithTariff(UUID.randomUUID(), "MISSING");

        assertThatThrownBy(() -> orderService.createOrder("idem-key-5", request))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(orderPersistenceService, paymentServiceGateway, subscriptionServiceGateway);
    }

    @Test
    void createOrder_whenIdempotencyKeyAlreadyCompleted_returnsCachedResponseWithoutRerunningSaga() {
        OrderResponse cached = new OrderResponse();
        cached.setId(UUID.randomUUID());
        cached.setStatus(Order.STATUS_FULFILLED);
        when(idempotencyKeyService.tryClaim(eq("idem-key-6"), any(), eq(OrderResponse.class)))
                .thenReturn(Optional.of(cached));

        OrderCreateRequest request = requestWithTariff(UUID.randomUUID(), "TARIFF100");
        OrderResponse response = orderService.createOrder("idem-key-6", request);

        assertThat(response).isSameAs(cached);
        verifyNoInteractions(customerServiceGateway, orderPersistenceService, paymentServiceGateway, subscriptionServiceGateway);
        verify(idempotencyKeyService, never()).complete(any(), any(), anyInt());
    }

    @Test
    void cancelOrder_whenAlreadyFulfilled_throwsInvalidOrderStateException() {
        Order order = existingOrder(UUID.randomUUID(), new BigDecimal("100.00"));
        order.setStatus(Order.STATUS_FULFILLED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(orderPersistenceService, never()).cancelOrder(any());
    }

    @Test
    void getOrderResponseById_whenMissing_throwsOrderNotFoundException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderResponseById(id)).isInstanceOf(OrderNotFoundException.class);
    }

    private OrderCreateRequest requestWithTariff(UUID customerId, String tariffCode) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductCode(tariffCode);
        item.setProductType("TARIFF");
        item.setQuantity(1);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerId(customerId);
        request.setItems(List.of(item));
        return request;
    }

    private OrderCreateRequest requestWithAddon(UUID customerId, String addonCode) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductCode(addonCode);
        item.setProductType("ADDON");
        item.setQuantity(1);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerId(customerId);
        request.setItems(List.of(item));
        return request;
    }

    private Order existingOrder(UUID customerId, BigDecimal totalAmount) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId(customerId);
        order.setStatus(Order.STATUS_PENDING_PAYMENT);
        order.setTotalAmount(totalAmount);
        order.setCurrency("TRY");
        order.setCreatedAt(Instant.now());
        return order;
    }

    private FeignException.NotFound notFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/resource",
                Collections.emptyMap(), null, StandardCharsets.UTF_8);
        Response response = Response.builder()
                .status(404)
                .request(request)
                .headers(Collections.emptyMap())
                .build();
        return (FeignException.NotFound) FeignException.errorStatus("Client#method()", response);
    }
}
