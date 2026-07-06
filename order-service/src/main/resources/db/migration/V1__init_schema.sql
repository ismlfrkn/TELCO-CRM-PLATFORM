CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    product_type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE saga_states (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    current_step VARCHAR(50) NOT NULL,
    payload TEXT,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_saga_states_order_id UNIQUE (order_id),
    CONSTRAINT fk_saga_states_order FOREIGN KEY (order_id) REFERENCES orders (id)
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

CREATE TABLE idempotency_keys (
    key VARCHAR(255) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    response_snapshot TEXT,
    http_status INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
