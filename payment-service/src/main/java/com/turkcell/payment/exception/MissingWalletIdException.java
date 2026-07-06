package com.turkcell.payment.exception;

public class MissingWalletIdException extends RuntimeException {
    public MissingWalletIdException(String message) {
        super(message);
    }
}
