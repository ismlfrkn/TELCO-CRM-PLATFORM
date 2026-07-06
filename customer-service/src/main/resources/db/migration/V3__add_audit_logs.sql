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
