CREATE TABLE bill_cycles (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL UNIQUE,
    day_of_month INTEGER NOT NULL,
    next_run_date DATE NOT NULL
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    subscription_id UUID NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    sub_total NUMERIC(19,2) NOT NULL,
    tax NUMERIC(19,2) NOT NULL,
    grand_total NUMERIC(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    due_date DATE NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_invoices_subscription_period UNIQUE (subscription_id, period_start, period_end)
);

CREATE INDEX idx_invoices_customer_id ON invoices (customer_id);

CREATE TABLE invoice_lines (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity NUMERIC(19,2) NOT NULL,
    unit_price NUMERIC(19,2) NOT NULL,
    line_total NUMERIC(19,2) NOT NULL,
    CONSTRAINT fk_invoice_lines_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
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
