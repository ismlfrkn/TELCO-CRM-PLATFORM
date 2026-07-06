package com.turkcell.usage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cdr_events")
public class CdrEvent {

    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_cdr_id", nullable = false, unique = true)
    private String externalCdrId; // CDR mediation sisteminin kendi kimligi - idempotency anahtari

    @Column(nullable = false)
    private String msisdn;

    @Column(name = "cdr_type", nullable = false)
    private String cdrType; // VOICE, SMS, DATA

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "duration_seconds")
    private Integer durationSeconds; // VOICE

    @Column(name = "data_volume_bytes")
    private Long dataVolumeBytes; // DATA

    @Column(name = "party_b")
    private String partyB;

    @Column(name = "network_type")
    private String networkType;

    @Column(nullable = false)
    private String status;

    @Column(name = "usage_record_id")
    private UUID usageRecordId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    public CdrEvent() {
    }

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
        if (status == null) {
            status = STATUS_RECEIVED;
        }
    }

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
