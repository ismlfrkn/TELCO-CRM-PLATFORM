package com.turkcell.productcatalog.exception;

public class TariffVersionNotFoundException extends RuntimeException {
    public TariffVersionNotFoundException(String message) {
        super(message);
    }
}
