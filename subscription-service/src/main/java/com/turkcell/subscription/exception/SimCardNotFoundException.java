package com.turkcell.subscription.exception;

public class SimCardNotFoundException extends RuntimeException {
    public SimCardNotFoundException(String message) {
        super(message);
    }
}
