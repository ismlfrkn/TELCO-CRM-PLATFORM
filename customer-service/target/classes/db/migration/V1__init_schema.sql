CREATE TABLE customers (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    identity_number VARCHAR(20) NOT NULL UNIQUE,
    date_of_birth DATE,
    email VARCHAR(255),
    phone VARCHAR(20),
    status VARCHAR(50) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE addresses (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    postal_code VARCHAR(20),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
);

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    file_ref VARCHAR(500) NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_documents_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
);
