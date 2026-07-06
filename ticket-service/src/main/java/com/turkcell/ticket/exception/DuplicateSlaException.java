package com.turkcell.ticket.exception;

public class DuplicateSlaException extends RuntimeException {
    public DuplicateSlaException(String message) {
        super(message);
    }
}
