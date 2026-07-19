package com.turkcell.payment.service;

public record MockPspResult(boolean success, String externalRef, String message) {

    public static MockPspResult success(String externalRef) {
        return new MockPspResult(true, externalRef, "Approved");
    }

    public static MockPspResult failure(String message) {
        return new MockPspResult(false, null, message);
    }
}
