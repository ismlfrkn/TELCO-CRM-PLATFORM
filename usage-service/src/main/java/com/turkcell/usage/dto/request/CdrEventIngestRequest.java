package com.turkcell.usage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class CdrEventIngestRequest {

    @NotBlank(message = "External CDR id cannot be blank")
    private String externalCdrId;

    @NotNull(message = "Subscription id cannot be null")
    private UUID subscriptionId;

    @NotBlank(message = "Msisdn cannot be blank")
    private String msisdn;

    @NotBlank(message = "Cdr type cannot be blank")
    private String cdrType;

    @NotNull(message = "Start time cannot be null")
    private Instant startTime;

    private Instant endTime;
    private Integer durationSeconds;
    private Long dataVolumeBytes;
    private String partyB;
    private String networkType;

    public String getExternalCdrId() { return externalCdrId; }
    public void setExternalCdrId(String externalCdrId) { this.externalCdrId = externalCdrId; }

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

    public String getCdrType() { return cdrType; }
    public void setCdrType(String cdrType) { this.cdrType = cdrType; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Long getDataVolumeBytes() { return dataVolumeBytes; }
    public void setDataVolumeBytes(Long dataVolumeBytes) { this.dataVolumeBytes = dataVolumeBytes; }

    public String getPartyB() { return partyB; }
    public void setPartyB(String partyB) { this.partyB = partyB; }

    public String getNetworkType() { return networkType; }
    public void setNetworkType(String networkType) { this.networkType = networkType; }
}
