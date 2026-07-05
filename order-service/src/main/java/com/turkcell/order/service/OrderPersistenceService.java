package com.turkcell.order.service;

import com.turkcell.order.entity.Order;
import com.turkcell.order.entity.OrderItem;
import com.turkcell.order.exception.OrderNotFoundException;
import com.turkcell.order.repository.OrderItemRepository;
import com.turkcell.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * OrderService'in DB yazma islemleri BILEREK ayri bir bean'e cikarildi: @Transactional, ayni sinif
 * icinde this.metod() ile (self-invocation) cagrildiginda Spring'in proxy'sini atlar ve SESSIZCE hicbir
 * transaction acmaz (identity-service'teki AuditLogService'ten beri bu projede dikkat edilen bir konu -
 * bkz. payment/usage/billing servislerindeki TransactionTemplate kullanimlari). Burada ayri bir bean
 * olmasi, gercek bir proxy siniri gectigi icin @Transactional'in dogru calismasini saglar.
 */
@Service
public class OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SagaStateService sagaStateService;

    public OrderPersistenceService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                                    SagaStateService sagaStateService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.sagaStateService = sagaStateService;
    }

    @Transactional
    public Order createOrderRecord(UUID customerId, BigDecimal totalAmount, List<OrderItem> items) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus(Order.STATUS_PENDING_PAYMENT);
        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemRepository.save(item);
        }

        sagaStateService.initialize(order.getId(), "ORDER_CREATED");
        return order;
    }

    @Transactional
    public void markAwaitingPayment(UUID orderId) {
        sagaStateService.advance(orderId, "AWAITING_PAYMENT");
    }

    @Transactional
    public void markOrderPaid(UUID orderId) {
        Order order = getOrderById(orderId);
        order.setStatus(Order.STATUS_PAID);
        orderRepository.save(order);
        sagaStateService.advance(orderId, "PAYMENT_COMPLETED");
    }

    @Transactional
    public void markOrderFulfilled(UUID orderId) {
        Order order = getOrderById(orderId);
        order.setStatus(Order.STATUS_FULFILLED);
        orderRepository.save(order);
        sagaStateService.advance(orderId, "COMPLETED");
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = getOrderById(orderId);
        order.setStatus(Order.STATUS_CANCELLED);
        orderRepository.save(order);
        sagaStateService.advance(orderId, "CANCELLED");
    }

    private Order getOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
    }
}
