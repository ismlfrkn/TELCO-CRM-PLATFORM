package com.turkcell.subscription.dto.response;

import java.time.Instant;

public class MsisdnPoolResponse {

    private String msisdn;
    private String status;
    private Instant reservedUntil;

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getReservedUntil() { return reservedUntil; }
    public void setReservedUntil(Instant reservedUntil) { this.reservedUntil = reservedUntil; }
}
