package com.turkcell.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * FR-30: kullanicinin bir kanaldan (SMS/EMAIL/PUSH) bildirim alip almak istemedigini tutar.
 * Kayit yoksa varsayilan opted-in kabul edilir (bkz. NotificationPreferenceService.isOptedIn) -
 * boylece mevcut senaryolar (welcome SMS vb.) musteri hicbir tercih belirtmeden calismaya devam eder.
 */
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "opted_in", nullable = false)
    private boolean optedIn;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NotificationPreference() {
    }

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getChannelId() { return channelId; }
    public void setChannelId(UUID channelId) { this.channelId = channelId; }

    public boolean isOptedIn() { return optedIn; }
    public void setOptedIn(boolean optedIn) { this.optedIn = optedIn; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
