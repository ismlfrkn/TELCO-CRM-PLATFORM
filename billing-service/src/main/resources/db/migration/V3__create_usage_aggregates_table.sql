CREATE TABLE usage_aggregates (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    quota_id UUID,
    cdr_type VARCHAR(20) NOT NULL,
    overage_quantity NUMERIC(19,2) NOT NULL,
    period_start DATE,
    period_end DATE,
    source_event_id UUID NOT NULL UNIQUE,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_usage_aggregates_subscription_id ON usage_aggregates (subscription_id);
