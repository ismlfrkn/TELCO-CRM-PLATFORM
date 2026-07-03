package com.turkcell.identity.exception;

public class DuplicatePermissionCodeException extends RuntimeException {
    public DuplicatePermissionCodeException(String message) {
        super(message);
    }
}
