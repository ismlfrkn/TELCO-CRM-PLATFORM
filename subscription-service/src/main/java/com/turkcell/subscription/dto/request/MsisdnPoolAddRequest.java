package com.turkcell.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MsisdnPoolAddRequest {

    @NotBlank(message = "Msisdn cannot be blank")
    @Pattern(regexp = "\\d{10,15}", message = "Msisdn must be 10 to 15 digits")
    private String msisdn;

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }
}
