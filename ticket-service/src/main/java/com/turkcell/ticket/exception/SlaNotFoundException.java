package com.turkcell.ticket.exception;

public class SlaNotFoundException extends RuntimeException {
    public SlaNotFoundException(String message) {
        super(message);
    }
}
