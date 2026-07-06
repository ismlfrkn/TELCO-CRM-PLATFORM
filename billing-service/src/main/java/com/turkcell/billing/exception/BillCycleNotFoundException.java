package com.turkcell.billing.exception;

public class BillCycleNotFoundException extends RuntimeException {
    public BillCycleNotFoundException(String message) {
        super(message);
    }
}
