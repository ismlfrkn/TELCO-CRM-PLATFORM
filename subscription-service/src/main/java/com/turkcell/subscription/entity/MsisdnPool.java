package com.turkcell.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "msisdn_pool")
public class MsisdnPool {

    @Id
    private String msisdn;

    @Column(nullable = false)
    private String status;

    @Column(name = "reserved_until")
    private Instant reservedUntil;

    public MsisdnPool() {
    }

    public MsisdnPool(String msisdn, String status, Instant reservedUntil) {
        this.msisdn = msisdn;
        this.status = status;
        this.reservedUntil = reservedUntil;
    }

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getReservedUntil() { return reservedUntil; }
    public void setReservedUntil(Instant reservedUntil) { this.reservedUntil = reservedUntil; }
}
