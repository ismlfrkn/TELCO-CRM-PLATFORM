package com.turkcell.billing.exception;

public class DuplicateBillCycleException extends RuntimeException {
    public DuplicateBillCycleException(String message) {
        super(message);
    }
}
