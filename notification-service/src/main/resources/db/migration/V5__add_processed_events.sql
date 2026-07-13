-- Consumer-side idempotency: outbox pattern "en az bir kez teslim" garantisi verdigi icin,
-- ayni event_id ile gelen bir mesaj iki kez islenmemeli.
CREATE TABLE processed_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    source_topic VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
