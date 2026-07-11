package com.turkcell.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class SubscriptionCreateRequest {

    @NotNull(message = "Customer id cannot be null")
    private UUID customerId;

    @NotBlank(message = "Tariff code cannot be blank")
    private String tariffCode;

    // FR-08: caller'in (ör. order-service) siparis anindaki tarife versiyonunu iletmesi icin;
    // opsiyoneldir, bu servis versiyonu dogrulamaz.
    private Integer tariffVersion;

    // Bos birakilirsa havuzdan otomatik musait bir numara tahsis edilir.
    private String msisdn;

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getTariffCode() { return tariffCode; }
    public void setTariffCode(String tariffCode) { this.tariffCode = tariffCode; }

    public Integer getTariffVersion() { return tariffVersion; }
    public void setTariffVersion(Integer tariffVersion) { this.tariffVersion = tariffVersion; }

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }
}
