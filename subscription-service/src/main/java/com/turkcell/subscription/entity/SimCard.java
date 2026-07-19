package com.turkcell.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sim_cards")
public class SimCard {

    @Id
    private String iccid;

    @Column(nullable = false, unique = true)
    private String imsi;

    @Column
    private String msisdn;

    @Column(nullable = false)
    private String status;

    public SimCard() {
    }

    public SimCard(String iccid, String imsi, String msisdn, String status) {
        this.iccid = iccid;
        this.imsi = imsi;
        this.msisdn = msisdn;
        this.status = status;
    }

    public String getIccid() { return iccid; }
    public void setIccid(String iccid) { this.iccid = iccid; }

    public String getImsi() { return imsi; }
    public void setImsi(String imsi) { this.imsi = imsi; }

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
