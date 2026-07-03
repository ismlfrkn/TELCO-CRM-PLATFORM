package com.turkcell.customer.exception;

public class DuplicateIdentityNumberException extends RuntimeException {
    public DuplicateIdentityNumberException(String message) {
        super(message);
    }
}
