-- FR-30: kullanici bazli kanal opt-in/opt-out tercihleri. Kayit yoksa uygulama katmaninda
-- varsayilan opted-in kabul edilir (bkz. NotificationPreferenceService.isOptedIn).
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    channel_id UUID NOT NULL,
    opted_in BOOLEAN NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_notification_preferences_channel FOREIGN KEY (channel_id) REFERENCES channels (id),
    CONSTRAINT uq_notification_preferences_user_channel UNIQUE (user_id, channel_id)
);

CREATE INDEX idx_notification_preferences_user_id ON notification_preferences (user_id);
