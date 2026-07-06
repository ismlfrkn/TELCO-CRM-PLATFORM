package com.turkcell.usage.exception;

public class DuplicateQuotaException extends RuntimeException {
    public DuplicateQuotaException(String message) {
        super(message);
    }
}
