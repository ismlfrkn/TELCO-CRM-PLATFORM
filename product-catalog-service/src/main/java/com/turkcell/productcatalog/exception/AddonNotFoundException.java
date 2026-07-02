package com.turkcell.productcatalog.exception;

public class AddonNotFoundException extends RuntimeException {
    public AddonNotFoundException(String message) {
        super(message);
    }
}
