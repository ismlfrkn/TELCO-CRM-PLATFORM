package com.turkcell.usage.dto.response;

import java.time.Instant;
import java.util.UUID;

public class CdrEventResponse {

    private UUID id;
    private String externalCdrId;
    private String msisdn;
    private String cdrType;
    private Instant startTime;
    private Instant endTime;
    private Integer durationSeconds;
    private Long dataVolumeBytes;
    private String partyB;
    private String networkType;
    private String status;
    private UUID usageRecordId;
    private String failureReason;
    private Instant receivedAt;
    private Instant processedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getExternalCdrId() { return externalCdrId; }
    public void setExternalCdrId(String externalCdrId) { this.externalCdrId = externalCdrId; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getUsageRecordId() { return usageRecordId; }
    public void setUsageRecordId(UUID usageRecordId) { this.usageRecordId = usageRecordId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
