CREATE TABLE tariffs (
    id UUID PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    monthly_fee DECIMAL(19, 2) NOT NULL,
    minutes_included INT NOT NULL,
    sms_included INT NOT NULL,
    data_mb_included INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE
);

CREATE TABLE addons (
    id UUID PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    validity_days INT NOT NULL
);

CREATE TABLE tariff_addons (
    tariff_id UUID NOT NULL,
    addon_id UUID NOT NULL,
    PRIMARY KEY (tariff_id, addon_id),
    CONSTRAINT fk_tariff_addons_tariff FOREIGN KEY (tariff_id) REFERENCES tariffs (id) ON DELETE CASCADE,
    CONSTRAINT fk_tariff_addons_addon FOREIGN KEY (addon_id) REFERENCES addons (id) ON DELETE CASCADE
);

CREATE TABLE product_offerings (
    id UUID PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    tariff_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    CONSTRAINT fk_product_offerings_tariff FOREIGN KEY (tariff_id) REFERENCES tariffs (id)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE
);
