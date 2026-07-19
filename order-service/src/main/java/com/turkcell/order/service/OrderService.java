package com.turkcell.order.service;

import com.turkcell.order.client.gateway.CustomerServiceGateway;
import com.turkcell.order.client.gateway.ProductCatalogServiceGateway;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final String AGGREGATE_TYPE = "Order";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPersistenceService orderPersistenceService;
    private final IdempotencyKeyService idempotencyKeyService;
    private final OutboxEventService outboxEventService;
    private final OrderMapper orderMapper;
    private final CustomerServiceGateway customerServiceGateway;
    private final ProductCatalogServiceGateway productCatalogServiceGateway;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                         OrderPersistenceService orderPersistenceService, IdempotencyKeyService idempotencyKeyService,
                         OutboxEventService outboxEventService, OrderMapper orderMapper,
                         CustomerServiceGateway customerServiceGateway,
                         ProductCatalogServiceGateway productCatalogServiceGateway) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderPersistenceService = orderPersistenceService;
        this.idempotencyKeyService = idempotencyKeyService;
        this.outboxEventService = outboxEventService;
        this.orderMapper = orderMapper;
        this.customerServiceGateway = customerServiceGateway;
        this.productCatalogServiceGateway = productCatalogServiceGateway;
    }

    public OrderResponse createOrder(String idempotencyKey, OrderCreateRequest request) {
        String requestHash = idempotencyKeyService.hash(request);
        Optional<OrderResponse> cached = idempotencyKeyService.tryClaim(idempotencyKey, requestHash, OrderResponse.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        OrderResponse response = initiateSaga(request);
        idempotencyKeyService.complete(idempotencyKey, response, 201);
        return response;
    }

    private OrderResponse initiateSaga(OrderCreateRequest request) {
        validateCustomerExists(request.getCustomerId());
        List<OrderItem> items = resolveItems(request.getItems());
        BigDecimal totalAmount = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = orderPersistenceService.createOrderRecord(request.getCustomerId(), totalAmount, items);
        OrderResponse response = toOrderResponse(order, items);
        outboxEventService.publish(AGGREGATE_TYPE, order.getId(), "OrderCreated", response);
        orderPersistenceService.markAwaitingPayment(order.getId());
        return response;
    }

    public OrderResponse getOrderResponseById(UUID id) {
        return toOrderResponse(getOrderById(id));
    }

    public Page<OrderResponse> getOrdersByCustomer(UUID customerId, Pageable pageable) {
        return orderRepository.findAllByCustomerId(customerId, pageable).map(this::toOrderResponse);
    }

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
            customerServiceGateway.getCustomer(customerId);
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
            return productCatalogServiceGateway.getTariff(code).getMonthlyFee();
        } catch (FeignException.NotFound ex) {
            throw new ProductNotFoundException("Tariff not found with code: " + code);
        }
    }

    private BigDecimal resolveAddonPrice(String code) {
        try {
            return productCatalogServiceGateway.getAddon(code).getPrice();
        } catch (FeignException.NotFound ex) {
            throw new ProductNotFoundException("Addon not found with code: " + code);
        }
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
