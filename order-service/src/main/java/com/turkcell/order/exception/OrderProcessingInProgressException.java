package com.turkcell.order.exception;

public class OrderProcessingInProgressException extends RuntimeException {
    public OrderProcessingInProgressException(String message) {
        super(message);
    }
}
