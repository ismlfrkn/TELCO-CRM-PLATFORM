package com.turkcell.customer.exception;

public class InvalidKycTransitionException extends RuntimeException {
    public InvalidKycTransitionException(String message) {
        super(message);
    }
}
