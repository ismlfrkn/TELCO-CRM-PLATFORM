CREATE TABLE quotas (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL UNIQUE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    minutes_remaining INTEGER NOT NULL,
    sms_remaining INTEGER NOT NULL,
    mb_remaining INTEGER NOT NULL,
    minutes_included INTEGER NOT NULL,
    sms_included INTEGER NOT NULL,
    mb_included INTEGER NOT NULL
);

CREATE TABLE usage_records (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity NUMERIC(19,2) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    cdr_ref VARCHAR(100)
);

CREATE INDEX idx_usage_records_subscription_id_recorded_at ON usage_records (subscription_id, recorded_at);

CREATE TABLE cdr_events (
    id UUID PRIMARY KEY,
    external_cdr_id VARCHAR(100) NOT NULL UNIQUE,
    msisdn VARCHAR(20) NOT NULL,
    cdr_type VARCHAR(20) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    duration_seconds INTEGER,
    data_volume_bytes BIGINT,
    party_b VARCHAR(20),
    network_type VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    usage_record_id UUID,
    failure_reason VARCHAR(500),
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_cdr_events_usage_record FOREIGN KEY (usage_record_id) REFERENCES usage_records (id)
);

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

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    action VARCHAR(100) NOT NULL,
    actor_id UUID,
    old_value TEXT,
    new_value TEXT,
    correlation_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
