package com.turkcell.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SimCardAssignRequest {

    @NotBlank(message = "Msisdn cannot be blank")
    private String msisdn;

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }
}
