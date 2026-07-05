-- V3__align_outbox_events_schema.sql
-- outbox_events tablosu hicbir zaman kullanilmadi (yazan bir OutboxEventService yoktu), bu yuzden
-- platformdaki diger servislerle (subscription, payment, usage, billing, notification, ticket, order)
-- ayni zengin semaya (status/retry_count/published_at/last_error) veri kaybi olmadan hizalanabilir.
DROP TABLE outbox_events;

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
