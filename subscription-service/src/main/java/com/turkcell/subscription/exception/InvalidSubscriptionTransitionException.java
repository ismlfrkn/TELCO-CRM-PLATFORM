package com.turkcell.subscription.exception;

public class InvalidSubscriptionTransitionException extends RuntimeException {
    public InvalidSubscriptionTransitionException(String message) {
        super(message);
    }
}
