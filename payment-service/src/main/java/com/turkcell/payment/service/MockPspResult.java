package com.turkcell.payment.service;

/**
 * Gercek bir PSP entegrasyonu MVP kapsami disinda (bkz. dokuman - "mock PSP"); bu tip, gercek bir
 * gateway'in donecegi decision+referans+mesaji temsil eden yerine gecen (mock) sonuc modelidir.
 */
public record MockPspResult(boolean success, String externalRef, String message) {

    public static MockPspResult success(String externalRef) {
        return new MockPspResult(true, externalRef, "Approved");
    }

    public static MockPspResult failure(String message) {
        return new MockPspResult(false, null, message);
    }
}
