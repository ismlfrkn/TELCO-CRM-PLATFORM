package com.turkcell.customer.exception;

public class InvalidIdentityNumberException extends RuntimeException {
    public InvalidIdentityNumberException(String message) {
        super(message);
    }
}
