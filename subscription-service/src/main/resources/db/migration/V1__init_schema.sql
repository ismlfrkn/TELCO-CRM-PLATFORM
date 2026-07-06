CREATE TABLE msisdn_pool (
    msisdn VARCHAR(20) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    reserved_until TIMESTAMP WITH TIME ZONE
);

CREATE TABLE sim_cards (
    iccid VARCHAR(30) PRIMARY KEY,
    imsi VARCHAR(20) NOT NULL UNIQUE,
    msisdn VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_sim_cards_msisdn FOREIGN KEY (msisdn) REFERENCES msisdn_pool (msisdn)
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    msisdn VARCHAR(20) NOT NULL,
    tariff_code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    activated_at TIMESTAMP WITH TIME ZONE,
    terminated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_subscriptions_msisdn FOREIGN KEY (msisdn) REFERENCES msisdn_pool (msisdn)
);

CREATE INDEX idx_subscriptions_customer_id ON subscriptions (customer_id);

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
