CREATE TABLE channels (
    id UUID PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE notification_templates (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    channel_id UUID NOT NULL,
    locale VARCHAR(10) NOT NULL,
    subject VARCHAR(255),
    body_template TEXT NOT NULL,
    CONSTRAINT fk_notification_templates_channel FOREIGN KEY (channel_id) REFERENCES channels (id),
    CONSTRAINT uq_notification_templates_code_channel_locale UNIQUE (code, channel_id, locale)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    template_code VARCHAR(100) NOT NULL,
    channel_id UUID NOT NULL,
    payload_json TEXT,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_notifications_channel FOREIGN KEY (channel_id) REFERENCES channels (id)
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    last_error TEXT
);

CREATE INDEX idx_outbox_events_status ON outbox_events (status);
