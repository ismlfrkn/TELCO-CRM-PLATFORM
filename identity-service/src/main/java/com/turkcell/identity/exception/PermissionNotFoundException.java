package com.turkcell.identity.exception;

public class PermissionNotFoundException extends RuntimeException {
    public PermissionNotFoundException(String message) {
        super(message);
    }
}
