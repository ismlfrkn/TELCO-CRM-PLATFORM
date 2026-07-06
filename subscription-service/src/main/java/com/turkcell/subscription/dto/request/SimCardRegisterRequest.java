package com.turkcell.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SimCardRegisterRequest {

    @NotBlank(message = "Iccid cannot be blank")
    private String iccid;

    @NotBlank(message = "Imsi cannot be blank")
    private String imsi;

    public String getIccid() { return iccid; }
    public void setIccid(String iccid) { this.iccid = iccid; }

    public String getImsi() { return imsi; }
    public void setImsi(String imsi) { this.imsi = imsi; }
}
