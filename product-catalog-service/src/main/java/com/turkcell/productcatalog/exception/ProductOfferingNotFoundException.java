package com.turkcell.productcatalog.exception;

public class ProductOfferingNotFoundException extends RuntimeException {
    public ProductOfferingNotFoundException(String message) {
        super(message);
    }
}
